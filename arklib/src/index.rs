mod android {
    macro_rules! wrap_error {
        ($env:expr, $body:expr) => {
            match $body {
                Ok(v) => v,
                Err(e) => {
                    // $env.throw(e).expect("error in throwing exception");
                    log::error!("{e}");
                    // $default
                    panic!()
                }
            }
        };
    }

    use arklib::{
        id::ResourceId,
        index::{IndexUpdate, ResourceIndex},
        meta::{ResourceKind, ResourceMeta},
    };
    use canonical_path::CanonicalPathBuf;
    use chrono::{TimeZone, Utc};
    use convert_case::{Case, Casing};

    use std::sync::MutexGuard;
    use std::{
        collections::{self, HashMap, HashSet},
        ffi::OsString,
        path::{Path, PathBuf},
        str::FromStr,
        time::SystemTime,
    };
    // use jni::errors::Result;
    use anyhow::Result;
    use jni::sys::{jlong, jobjectArray};
    use jni::{
        descriptors::Desc,
        objects::{JClass, JMap, JObject, JString, JValue},
    };
    use jni::{objects::AutoLocal, JNIEnv};
    use jni::{objects::JMapIter, sys::jboolean};
    use jni::{
        objects::{self, JList},
        sys::jobject,
    };
    use jni::{signature::JavaType, strings::JNIString};
    use jni::{
        strings::JNIStr,
        sys::{JNI_FALSE, JNI_TRUE},
    };
    use jni_fn::jni_fn;

    use crate::signature::STRING;
    use crate::{
        interop::{self, INNER_PTR_FIELD},
        signature,
    };

    struct RustResourcesIndex;

    fn get_index<'a>(env: &'a JNIEnv<'a>, this: JObject<'a>) -> MutexGuard<'a, ResourceIndex> {
        let val = env
            .get_rust_field::<_, _, ResourceIndex>(this, INNER_PTR_FIELD)
            .unwrap();
        val
    }

    // Get `java/lang/String` and parse into `String`
    fn get_string_field<'a, O, S: Into<JNIString>>(env: JNIEnv, obj: O, field: S) -> Option<String>
    where
        O: Into<JObject<'a>>,
    {
        let raw = JString::from(
            env.get_field(obj.into(), field.into(), "Ljava/lang/String;")
                .unwrap()
                .l()
                .unwrap(),
        );

        env.get_string(raw).ok().map(|s| s.into())
    }
    // Get Interger and parse into `i64`
    fn get_integer_field<'a, O, S: Into<JNIString>>(env: JNIEnv, obj: O, field: S) -> Option<i64>
    where
        O: Into<JObject<'a>>,
    {
        let integer_cls = env.find_class("java/lang/Integer").unwrap();
        let raw = env
            .get_field(obj.into(), field.into(), signature::INTEGER)
            .unwrap()
            .l()
            .unwrap();
        if raw.is_null() {
            return None;
        }

        Some(
            env.call_method(integer_cls, "longValue", "()J", &[])
                .unwrap()
                .j()
                .unwrap(),
        )
    }
    fn from_java_resource_meta(env: JNIEnv, meta: JObject) -> Result<ResourceMeta> {
        let id = env.get_field(meta, "id", "J")?.j()?;

        let name = get_string_field(env, meta, "name").map(|s| s.into());
        let extension = get_string_field(env, meta, "extension").map(|s| s.into());
        // Modified Field Transform
        let modified_fn = env
            .get_field(meta, "modified", "Ljava/nio/file/attribute/FileTime;")?
            .l()?;
        let modified_val = env.call_method(modified_fn, "toMillis", "()J", &[])?.j()?;
        let modified = Utc.timestamp_millis(modified_val);

        let size = env.get_field(meta, "size", "J")?.j()?;

        let kind = env
            .get_field(meta, "kind", "Lspace/taran/arklib/index/ResourceKind;")?
            .l()?;

        if kind.is_null() {
            return Ok(ResourceMeta {
                id: ResourceId {
                    file_size: size as u64,
                    crc32: id as u32,
                },
                name,
                extension,
                modified,
                kind: None,
            });
        }

        let kind_code = env
            .call_method(
                kind,
                "getCode",
                "()Lspace/taran/arklib/index/KindCode;",
                &[],
            )?
            .l()?;
        let kind_code_name: String = env
            .get_string(JString::from(
                env.call_method(kind_code, "toString", "()Ljava/lang/String;", &[])?
                    .l()?,
            ))?
            .into();

        let rk_ty = "space/taran/arklib/index/ResourceKind";
        let rk = ResourceKind::from_str(&kind_code_name)
            .map::<Result<ResourceKind>, _>(|s| match s {
                ResourceKind::Document { pages: _ } => {
                    let kind = env
                        .get_field(meta, "kind", format!("{rk_ty}$Document"))?
                        .l()?;
                    let pages = get_integer_field(env, kind, "pages");
                    Ok(ResourceKind::Document { pages })
                }
                ResourceKind::Link {
                    title: _,
                    description: _,
                    url: _,
                } => {
                    let kind = env.get_field(meta, "kind", format!("{rk_ty}$Link"))?.l()?;
                    let title = get_string_field(env, kind, "title");
                    let description = get_string_field(env, kind, "description");
                    let url = get_string_field(env, kind, "url");
                    Ok(ResourceKind::Link {
                        title,
                        description,
                        url,
                    })
                }
                ResourceKind::Video {
                    height: _,
                    width: _,
                    duration: _,
                } => {
                    let kind = env.get_field(meta, "kind", format!("{rk_ty}$Video"))?.l()?;
                    let height = get_integer_field(env, kind, "height");
                    let width = get_integer_field(env, kind, "width");
                    let duration = get_integer_field(env, kind, "duration");
                    Ok(ResourceKind::Video {
                        height,
                        width,
                        duration,
                    })
                }
                _ => Ok(s),
            })
            .unwrap()
            .unwrap();

        Ok(ResourceMeta {
            id: ResourceId {
                file_size: size as u64,
                crc32: id as u32,
            },
            name,
            extension,
            modified,
            kind: Some(rk),
        })
    }
    fn into_java_resource_meta(
        env: JNIEnv,
        meta: ResourceMeta,
    ) -> Result<JObject, jni::errors::Error> {
        let resource_meta_cls = env.find_class("space/taran/arklib/index/ResourceMeta")?;
        let filetime_cls = env.find_class("java/nio/file/attribute/FileTime")?;
        log::info!("converting resource meta");
        //         let resource_meta_constructor = env.get_method_id(resource_meta_cls, "<init>", "(JLjava/lang/String;Ljava/lang/String;Ljava/nio/file/attribute/FileTime;JLspace/taran/arklib/index/ResourceKind;)V
        // ")?;

        let rk_ty = "space/taran/arklib/index/ResourceKind";
        let val = meta;
        let id = val.id.crc32 as i64;
        let name = env.new_string(
            val.name
                .unwrap_or(OsString::from(""))
                .into_string()
                .unwrap(),
        )?;
        let extension = env.new_string(
            val.extension
                .unwrap_or(OsString::from(""))
                .into_string()
                .unwrap(),
        )?;
        let modified = env.call_static_method(
            filetime_cls,
            "fromMillis",
            "(J)Ljava/nio/file/attribute/FileTime;",
            &[JValue::Long(val.modified.timestamp_millis())],
        )?;
        let size = val.id.file_size as i64;
        let kind = val.kind.unwrap_or(ResourceKind::PlainText);
        let kind_ty = format!("{rk_ty}${}", kind.to_string());
        log::debug!("Type: {kind_ty}");
        let kind_cls = env.find_class(kind_ty)?;
        let kind = match kind {
            ResourceKind::Document { pages } => {
                let pages = if let Some(pages) = pages {
                    let long_cls = env.find_class("java/lang/Long")?;
                    env.new_object(long_cls, "(J)V", &[JValue::Long(pages)])?
                } else {
                    log::info!("page is None, return null");
                    JObject::null()
                };
                log::info!("creating kind object");
                env.new_object(kind_cls, "(Ljava/lang/Long;)V", &[JValue::from(pages)])?
            }
            ResourceKind::Link {
                title,
                description,
                url,
            } => {
                let to_java = |string: Option<String>| match string {
                    Some(v) => Ok::<JObject<'_>, jni::errors::Error>(env.new_string(v)?.into()),
                    None => Ok(JObject::null()),
                };
                let title = to_java(title)?;
                let description = to_java(description)?;
                let url = to_java(url)?;
                env.new_object(
                    kind_cls,
                    format!("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
                    &[
                        JValue::from(title),
                        JValue::from(description),
                        JValue::from(url),
                    ],
                )?
            }
            ResourceKind::Video {
                height,
                width,
                duration,
            } => {
                let long_cls = env.find_class("java/lang/Long")?;
                let method_id = env.get_method_id(long_cls, "<init>", "(J)V")?;
                let to_java = |v: Option<i64>| match v {
                    Some(v) => env.new_object_unchecked(long_cls, method_id, &[JValue::Long(v)]),
                    None => Ok(JObject::null()),
                };
                let height = to_java(height)?;
                let width = to_java(width)?;
                let duration = to_java(duration)?;
                env.new_object(
                    kind_cls,
                    format!("(Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Long;)V"),
                    &[
                        JValue::from(height),
                        JValue::from(width),
                        JValue::from(duration),
                    ],
                )?
            }
            _ => env.new_object(kind_cls, format!("()V"), &[])?,
        };
        env.new_object(
            resource_meta_cls,
            "(JLjava/lang/String;Ljava/lang/String;Ljava/nio/file/attribute/FileTime;JLspace/taran/arklib/index/ResourceKind;)V",
            &[
                JValue::Long(id),
                JValue::from(name),
                JValue::from(extension),
                modified,
                JValue::Long(size),
                JValue::from(kind),
            ],
        )
    }

    fn to_java_path(env: JNIEnv, path: String) -> Result<JObject, jni::errors::Error> {
        let paths_cls = env.find_class("java/nio/file/Paths")?;
        let sig = "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;";
        let string_cls = env.find_class("java/lang/String")?;
        let method_id = env.get_static_method_id(paths_cls, "get", sig)?;
        log::info!("path: {path}");
        let path_val = env.new_string(path)?;
        let n = env.new_object_array(0, string_cls, JObject::null())?;
        env.call_static_method_unchecked(
            paths_cls,
            method_id,
            JavaType::Object(String::from("java/nio/file/Path")),
            &[JValue::from(path_val), JValue::from(n)],
        )?
        .l()
    }
    fn from_java_path(env: JNIEnv, path: JObject) -> Result<CanonicalPathBuf> {
        log::info!("converting from java path");

        let path = env
            .call_method(path, "toString", "()Ljava/lang/String;", &[])?
            .l()?;

        let path_obj = JString::from(path);

        let path: String = env.get_string(path_obj)?.into();
        Ok(CanonicalPathBuf::canonicalize(path)?)
    }

    impl RustResourcesIndex {
        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn init(env: JNIEnv, _this: JObject, root_path: JString, resources: JObject) -> jlong {
            let resources = match env.get_map(resources) {
                Ok(v) => v,
                Err(e) => {
                    log::error!("{e}");
                    panic!()
                }
            };

            let root_path: String = env.get_string(root_path).unwrap().into();
            let res_iter = resources.iter().unwrap();
            let res = res_iter
                .map(|(a, b)| {
                    let path = wrap_error!(env, from_java_path(env, a));
                    let path = CanonicalPathBuf::new(path).unwrap();

                    let meta = wrap_error!(env, from_java_resource_meta(env, b));
                    (path, meta)
                })
                .collect();
            let ri = ResourceIndex::from_resources(root_path, res);
            interop::into_raw::<ResourceIndex>(ri)
        }

        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn listResources(env: JNIEnv, this: JObject, jni_prefix: JString) -> jobjectArray {
            let ri = get_index(&env, this);
            let linked_hashmap_cls = env.find_class("java/util/LinkedHashMap").unwrap();

            let jmap = env
                .get_map(env.new_object(linked_hashmap_cls, "()V", &[]).unwrap())
                .unwrap();

            if !jni_prefix.is_null() {
                let prefix: String = env.get_string(jni_prefix).unwrap().into();
                log::info!("getting resources in prefix: {prefix}");
                ri.path2meta
                    .iter()
                    .filter(|(path, _)| path.starts_with(prefix.clone()))
                    .map(|(a, b)| (a.clone(), b.clone()))
                    .collect()
            } else {
                log::info!("prefix is none, use default");
                ri.path2meta.clone()
            }
            .iter()
            .for_each(|(path, meta)| {
                let path = wrap_error!(env, to_java_path(env, path.to_str().unwrap().to_string()));
                let meta = wrap_error!(env, into_java_resource_meta(env, meta.clone()));
                jmap.put(path, meta).unwrap();
            });
            jmap.into_inner()
        }
        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn getPath(env: JNIEnv, this: JObject, id: jlong) -> jobject {
            let ri = get_index(&env, this);
            log::info!("getting path by id: {id}");
            let val = ri.path2meta.iter().find(|&x| x.1.id.crc32 as i64 == id);
            match val {
                Some(val) => {
                    let path = val.0.as_path().to_str().unwrap().to_string();
                    let wrapper = wrap_error!(env, to_java_path(env, path));
                    wrapper.into_inner()
                }
                None => JObject::null().into_inner(),
            }
        }
        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn getMeta(env: JNIEnv, this: JObject, id: jlong) -> jobject {
            let ri = get_index(&env, this);
            let val = ri.path2meta.iter().find(|&x| x.1.id.crc32 as i64 == id);
            match val {
                Some(val) => {
                    wrap_error!(env, into_java_resource_meta(env, val.1.clone())).into_inner()
                }
                None => JObject::null().into_inner(),
            }
        }
        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn reindex(env: JNIEnv, this: JObject) -> jobject {
            let mut ri = get_index(&env, this);
            log::info!("reindexing...");
            let diff = ri.update().unwrap();
            log::info!("got diff");

            let linked_hashmap_cls = env.find_class("java/util/LinkedHashMap").unwrap();

            let deleted = env
                .get_map(env.new_object(linked_hashmap_cls, "()V", &[]).unwrap())
                .unwrap();
            let added = env
                .get_map(env.new_object(linked_hashmap_cls, "()V", &[]).unwrap())
                .unwrap();

            for (path, meta) in diff.deleted.iter() {
                log::info!("add deleted: {}", path.as_path().to_str().unwrap());
                let path = wrap_error!(env, to_java_path(env, path.to_str().unwrap().to_string()));
                let meta = wrap_error!(env, into_java_resource_meta(env, meta.clone()));
                deleted.put(path, meta).unwrap();
            }

            for (path, meta) in diff.added.iter() {
                log::info!("add added: {}", path.as_path().to_str().unwrap());
                let path = wrap_error!(env, to_java_path(env, path.to_str().unwrap().to_string()));
                let meta = wrap_error!(env, into_java_resource_meta(env, meta.clone()));
                added.put(path, meta).unwrap();
            }
            log::info!("creating difference");
            let difference_cls = env
                .find_class("space/taran/arklib/index/Difference")
                .unwrap();
            env.new_object(
                difference_cls,
                "(Ljava/util/Map;Ljava/util/Map;)V",
                &[JValue::from(deleted), JValue::from(added)],
            )
            .unwrap()
            .into_inner()
        }
        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn remove(env: JNIEnv, this: JObject, id: jlong) -> jobject {
            let mut ri = get_index(&env, this);
            log::info!("removing id: {id}");
            log::info!("Index: {:#?}", ri.path2meta.clone());
            let cl = ri.path2meta.clone();
            let iter = cl.into_iter();
            let mut pair_iter = iter.filter(|(_, meta)| meta.id.crc32 == id as u32);
            let val = pair_iter.next();
            log::info!("Removed: {:#?}", val);
            let obj = match val {
                Some((path, meta_removed)) => {
                    ri.path2meta.remove(path.as_canonical_path()).unwrap();
                    let wrapper =
                        wrap_error!(env, to_java_path(env, path.to_str().unwrap().to_string()))
                            .into_inner();
                    wrapper
                }
                None => {
                    log::error!("given id not found: {}", id);
                    JObject::null().into_inner()
                }
            };

            obj
        }
        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn updateResource(env: JNIEnv, this: JObject, path: JObject, new_resource: JObject) {
            log::info!("updating resources");
            let mut ri = get_index(&env, this);
            let path = wrap_error!(env, from_java_path(env, path));
            let new_res = wrap_error!(env, from_java_resource_meta(env, new_resource));
            match ri.path2meta.insert(path, new_res.clone()) {
                Some(v) => {
                    log::info!("updated resource: {}", v.id.crc32);
                }
                None => {
                    log::warn!(
                        "resource not found, added the resource: {}",
                        new_res.id.crc32
                    );
                }
            };
        }

        #[jni_fn("space.taran.arklib.index.RustResourcesIndex")]
        pub fn contains(env: JNIEnv, this: JObject, id: jlong) -> jboolean {
            let ri = get_index(&env, this);
            match ri.path2meta.values().find(|x| x.id.crc32 == id as u32) {
                Some(_) => JNI_TRUE,
                None => JNI_FALSE,
            }
        }
    }
}
