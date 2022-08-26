mod android {
    use std::{
        collections::{self, HashMap},
        path::{Path, PathBuf},
        str::FromStr,
        time::SystemTime,
    };

    use arklib::{
        id::ResourceId,
        index::{IndexUpdate, ResourceIndex},
        meta::{ResourceKind, ResourceMeta},
    };
    use canonical_path::CanonicalPathBuf;
    use chrono::{TimeZone, Utc};
    use convert_case::{Case, Casing};
    use jni::sys::jobject;
    use jni::sys::{jlong, jobjectArray};
    use jni::{
        descriptors::Desc,
        objects::{JClass, JMap, JObject, JString, JValue},
    };
    use jni::{objects::AutoLocal, JNIEnv};
    use jni::{signature::JavaType, strings::JNIString};
    use jni_fn::jni_fn;
    use paste::paste;

    use crate::{
        interop::{self, INNER_PTR_FIELD},
        signature,
    };

    struct RustResourcesIndex;

    fn get_index(env: &JNIEnv, this: JObject) -> ResourceIndex {
        let ptr = env
            .get_rust_field::<_, _, jlong>(this, INNER_PTR_FIELD)
            .unwrap();
        interop::from_raw::<ResourceIndex>(*ptr).unwrap()
    }

    // Get `java/lang/String` and parse into `String`
    fn get_string_field<'a, O, S: Into<JNIString>>(env: &JNIEnv, obj: O, field: S) -> String
    where
        O: Into<JObject<'a>>,
    {
        let raw = JString::from(
            env.get_field(obj.into(), field.into(), "Ljava/lang/String;")
                .unwrap()
                .l()
                .unwrap(),
        );
        if raw.is_null() {
            return "".into();
        }
        env.get_string(raw).unwrap().into()
    }
    // Get Interger and parse into `i64`
    fn get_integer_field<'a, O, S: Into<JNIString>>(env: &JNIEnv, obj: O, field: S) -> i64
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
            return 0;
        }

        env.call_method(integer_cls, "longValue", "()J", &[])
            .unwrap()
            .j()
            .unwrap()
    }
    impl RustResourcesIndex {
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_init(
            env: &JNIEnv,
            _clazz: JClass,
            root_path: JString,
            resources: JObject,
        ) -> jlong {
            let resources = JMap::from_env(env, resources).unwrap();
            let root_path: String = env.get_string(root_path).unwrap().into();
            let res_iter = resources.iter().unwrap();
            let res = res_iter
                .map(|(a, b)| {
                    let path: String = env.get_string(JString::from(a)).unwrap().into();
                    let path = CanonicalPathBuf::new(path).unwrap();
                    let id = env.get_field(b, "id", "J").unwrap().j().unwrap();

                    let name = get_string_field(env, b, "name").into();
                    let extension = get_string_field(env, b, "extension").into();
                    // Modified Field Transform
                    let modified_fn = env
                        .get_field(b, "modified", "Ljava/nio/file/attribute/FileTime;")
                        .unwrap()
                        .l()
                        .unwrap();
                    let modified_val = env
                        .call_method(modified_fn, "toMillis", "()J", &[])
                        .unwrap()
                        .j()
                        .unwrap();
                    let modified = Utc.timestamp_millis(modified_val);

                    let size = env.get_field(b, "size", "J").unwrap().j().unwrap();

                    let kind = env
                        .get_field(b, "kind", "Lspace/taran/arklib/index/ResourceKind;")
                        .unwrap()
                        .l()
                        .unwrap();
                    let kind_code = env
                        .call_method(
                            kind,
                            "getCode",
                            "()Lspace/taran/arklib/index/KindCode;",
                            &[],
                        )
                        .unwrap()
                        .l()
                        .unwrap();
                    let kind_code_name: String = env
                        .get_string(JString::from(
                            env.call_method(kind_code, "toString", "()Ljava/lang/String;", &[])
                                .unwrap()
                                .l()
                                .unwrap(),
                        ))
                        .unwrap()
                        .into();

                    let rk_ty = "space/taran/arklib/index/ResourceKind";
                    let rk = ResourceKind::from_str(&kind_code_name)
                        .map(|s| match s {
                            ResourceKind::Document { pages: _ } => {
                                // let doc_cls = env.find_class(format!("{rk_ty}$Document")).unwrap();
                                let kind = env
                                    .get_field(b, "kind", format!("{rk_ty}$Document"))
                                    .unwrap()
                                    .l()
                                    .unwrap();
                                let pages = get_integer_field(env, kind, "pages");
                                ResourceKind::Document { pages }
                            }
                            ResourceKind::Link {
                                title: _,
                                description: _,
                                url: _,
                            } => {
                                let kind = env
                                    .get_field(b, "kind", format!("{rk_ty}$Link"))
                                    .unwrap()
                                    .l()
                                    .unwrap();
                                let title = get_string_field(env, kind, "title");
                                let description = get_string_field(env, kind, "description");
                                let url = get_string_field(env, kind, "url");
                                ResourceKind::Link {
                                    title,
                                    description,
                                    url,
                                }
                            }
                            ResourceKind::Video {
                                height: _,
                                width: _,
                                duration: _,
                            } => {
                                let kind = env
                                    .get_field(b, "kind", format!("{rk_ty}$Video"))
                                    .unwrap()
                                    .l()
                                    .unwrap();
                                let height = get_integer_field(env, kind, "height");
                                let width = get_integer_field(env, kind, "width");
                                let duration = get_integer_field(env, kind, "duration");
                                ResourceKind::Video {
                                    height,
                                    width,
                                    duration,
                                }
                            }
                            _ => s,
                        })
                        .unwrap();

                    let meta = ResourceMeta {
                        id: ResourceId {
                            file_size: size as u64,
                            crc32: id as u32,
                        },
                        name: Some(name),
                        extension: Some(extension),
                        modified,
                        kind: Some(rk),
                    };
                    (path, meta)
                })
                .collect();
            let ri = ResourceIndex::from_resources(root_path, res);
            interop::into_raw::<ResourceIndex>(ri)
        }

        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_listResources(
            env: &JNIEnv,
            _clazz: JClass,
            this: JObject,
            jni_prefix: JString,
        ) -> jobjectArray {
            let ri = get_index(env, this);
            let resource_meta_cls = env
                .find_class("space/taran/arklib/index/ResourceMeta")
                .unwrap();
            let filetime_cls = env.find_class("java/nio/file/attribute/FileTime").unwrap();
            let resource_meta_constructor = env.get_method_id(resource_meta_cls, "<init>", "(JLjava/lang/String;Ljava/lang/String;Ljava/nio/file/attribute/FileTime;JLspace/taran/arklib/index/ResourceKind;)V
").unwrap();
            let rk_ty = "space/taran/arklib/index/ResourceKind";
            let val = if !jni_prefix.is_null() {
                let prefix: String = env.get_string(jni_prefix).unwrap().into();
                ri.path2meta
                    .iter()
                    .filter(|(path, _)| path.starts_with(prefix.clone()))
                    .map(|(a, b)| (a.clone(), b.clone()))
                    .collect()
            } else {
                ri.path2meta
            }
            .values()
            .map(|val| {
                let id = val.id.crc32 as i64;
                let name = env
                    .new_string(val.name.unwrap_or_default().into_string().unwrap())
                    .unwrap();
                let extension = env
                    .new_string(val.extension.unwrap_or_default().into_string().unwrap())
                    .unwrap();
                let modified = env
                    .call_static_method(
                        filetime_cls,
                        "fromMillis",
                        "(J)Ljava/nio/file/attribute/FileTime;",
                        &[JValue::Long(val.modified.timestamp_millis())],
                    )
                    .unwrap();
                let size = val.id.file_size as i64;
                let kind = val.kind.unwrap_or(ResourceKind::PlainText);
                let kind_ty = format!("{rk_ty}${}", kind.to_string());
                let kind_cls = env.find_class(kind_ty).unwrap();
                let kind = match kind {
                    ResourceKind::Document { pages } => {
                        let long_cls = env.find_class("java/lang/Long").unwrap();
                        let pages = env
                            .new_object(long_cls, "(J)V", &[JValue::Long(pages)])
                            .unwrap();
                        env.new_object(
                            kind_cls,
                            format!("(Ljava/lang/Long;)V"),
                            &[JValue::from(pages)],
                        )
                        .unwrap()
                    }
                    ResourceKind::Link {
                        title,
                        description,
                        url,
                    } => {
                        let title = env.new_string(title).unwrap();
                        let description = env.new_string(description).unwrap();
                        let url = env.new_string(url).unwrap();
                        env.new_object(
                            kind_cls,
                            format!("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
                            &[
                                JValue::from(title),
                                JValue::from(description),
                                JValue::from(url),
                            ],
                        )
                        .unwrap()
                    }
                    ResourceKind::Video {
                        height,
                        width,
                        duration,
                    } => {
                        let long_cls = env.find_class("java/lang/Long").unwrap();
                        let method_id = env.get_method_id(long_cls, "<init>", "(J)V").unwrap();
                        let height = env
                            .new_object_unchecked(long_cls, method_id, &[JValue::Long(height)])
                            .unwrap();
                        let width = env
                            .new_object_unchecked(long_cls, method_id, &[JValue::Long(width)])
                            .unwrap();
                        let duration = env
                            .new_object_unchecked(long_cls, method_id, &[JValue::Long(duration)])
                            .unwrap();
                        env.new_object(
                            kind_cls,
                            format!("(Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Long;)V"),
                            &[
                                JValue::from(height),
                                JValue::from(width),
                                JValue::from(duration),
                            ],
                        )
                        .unwrap()
                    }
                    _ => env.new_object(kind_cls, format!("()V"), &[]).unwrap(),
                };
                env.new_object_unchecked(
                    resource_meta_cls,
                    resource_meta_constructor,
                    &[
                        JValue::Long(id),
                        JValue::from(name),
                        JValue::from(extension),
                        modified,
                        JValue::Long(size),
                        JValue::from(kind),
                    ],
                )
                .unwrap()
            })
            .collect::<Vec<_>>();

            let default_filetime = env
                .call_static_method(
                    filetime_cls,
                    "fromMillis",
                    "(J)Ljava/nio/file/attribute/FileTime;",
                    &[JValue::Long(0)],
                )
                .unwrap()
                .l()
                .unwrap();
            let default_string = env.new_string("").unwrap();

            let default_resource_meta = env.new_object(resource_meta_cls, "(ILjava/lang/String;Ljava/lang/String;Ljava/nio/file/attribute/FileTime;ILspace/taran/arklib/index/ResourceKind;)", &[
                JValue::Long(1),
                JValue::from(default_string),
                JValue::from(default_string),
                JValue::from(default_filetime),
                JValue::Long(1)]).unwrap();
            let obj = env
                .new_object_array(
                    val.len().try_into().unwrap(),
                    resource_meta_cls,
                    default_resource_meta,
                )
                .unwrap();
            let mut i = 0;
            for elem in val.iter() {
                env.set_object_array_element(obj, i, *elem);
                i += i
            }

            obj
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_getPath(
            env: &JNIEnv,
            _clazz: JClass,
            _this: JObject,
            id: jlong,
        ) -> JString {
            JString::from("")
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_getMeta(
            env: &JNIEnv,
            _clazz: JClass,
            _this: JObject,
            id: jlong,
        ) -> JObject {
            todo!()
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_reindex(env: &JNIEnv, this: JObject) {
            let mut ri = env
                .get_rust_field::<_, _, ResourceIndex>(this, INNER_PTR_FIELD)
                .unwrap();
            ri.update();
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_remove(env: &JNIEnv, id: jlong, this: JObject) {
            let mut ri = interop::get_inner::<ResourceIndex>(env, this).unwrap();
            ri.path2meta.retain(|_, v| id == v.id.crc32.into());
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_updateResource() {}
    }
}
