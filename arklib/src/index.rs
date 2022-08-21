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
    use jni::sys::jlong;
    use jni::sys::jobject;
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
                                let doc_cls = env.find_class(format!("{rk_ty}$Document")).unwrap();
                                let pages = get_integer_field(env, doc_cls, "pages");
                                ResourceKind::Document { pages }
                            }
                            ResourceKind::Link {
                                title: _,
                                description: _,
                                url: _,
                            } => {
                                let link_cls = env.find_class(format!("{rk_ty}$Link")).unwrap();

                                let title = get_string_field(env, link_cls, "title");
                                let description = get_string_field(env, link_cls, "description");
                                let url = get_string_field(env, link_cls, "url");
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
                                let video_cls = env.find_class(format!("{rk_ty}$Video")).unwrap();
                                let height = get_integer_field(env, video_cls, "height");
                                let width = get_integer_field(env, video_cls, "width");
                                let duration = get_integer_field(env, video_cls, "duration");
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
        ) -> jobject {
            let ptr = env
                .get_rust_field::<_, _, jlong>(this, INNER_PTR_FIELD)
                .unwrap();
            let ri = interop::from_raw::<ResourceIndex>(*ptr).unwrap();
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
            .map(|val| val.clone())
            .collect::<Vec<_>>();

            let resource_meta_cls = env
                .find_class("space/taran/arklib/index/ResourceMeta")
                .unwrap();

            let default_resource_meta = env.new_object(resource_meta_cls, "(ILjava/lang/String;Ljava/lang/String;Ljava/nio/file/attribute/FileTime;ILspace/taran/arklib/index/ResourceKind;)", &[JValue::Long(1)]).unwrap();
            let obj = env
                .new_object_array(
                    val.len().try_into().unwrap(),
                    resource_meta_cls,
                    default_resource_meta,
                )
                .unwrap();

            obj
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_getPath(
            env: &JNIEnv,
            _clazz: JClass,
            _this: JObject,
            id: jlong,
        ) -> String {
            todo!()
        }
        #[jni_fn("space.taran.arklib.index")]
        pub fn RustResourcesIndex_getMeta(
            env: &JNIEnv,
            _clazz: JClass,
            _this: JObject,
            id: jlong,
        ) -> jobject {
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
