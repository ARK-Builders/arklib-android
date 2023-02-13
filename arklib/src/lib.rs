#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use arklib::index::ResourceIndex;
    use jni::objects::{JClass, JObject, JString, JValue};
    use jni::sys::{jboolean, jint, jlong, jobject, jstring, JNI_TRUE, JNI_FALSE};
    use jni::JNIEnv;
    use std::time::{SystemTime, UNIX_EPOCH};
    use log::{debug, trace, Level};
    use std::collections::HashMap;
    use std::path::PathBuf;
    use std::sync::Mutex;
    use std::{fs::File, path::Path};
    extern crate android_logger;
    use once_cell::sync::Lazy;
    use android_logger::Config;
    use arklib::id::ResourceId;
    use arklib::link::Link;
    use arklib::pdf::PDFQuality;
    use image::EncodableLayout;
    use jni::signature::{JavaType, Primitive};
    use url::Url;

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_initRustLogger(_: JNIEnv, _: JClass) {
        android_logger::init_once(Config::default().with_min_level(Level::Trace));
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_computeIdNative(
        env: JNIEnv,
        _: JClass,
        jni_size: i64,
        jni_file_name: JString,
    ) -> jobject {
        let data_size: usize =
            usize::try_from(jni_size).unwrap_or_else(|_| panic!("Failed to parse input size"));
        println!("Received size: {}", data_size);
        let file_name: String = env
            .get_string(jni_file_name)
            .expect("Failed to parse input file name")
            .into();
        let file_path: &Path = Path::new(&file_name);
        trace!("Received filename: {}", file_path.display());

        let resourceId = ResourceId::compute(data_size.try_into().unwrap(), file_path).unwrap();

        let resource_id_cls = env.find_class("space/taran/arklib/ResourceId").unwrap();

        let create_resource_id_fn = env
            .get_static_method_id(
                resource_id_cls,
                "create",
                "(JJ)Lspace/taran/arklib/ResourceId;",
            )
            .unwrap();

        let data_size: jlong = resourceId.data_size as usize as i64;
        let crc32: jlong = resourceId.crc32 as usize as i64;

        trace!("after uszie");
        let resource_id = env
            .call_static_method_unchecked(
                resource_id_cls,
                create_resource_id_fn,
                JavaType::Object(String::from("space/taran/arklib/ResourceId")),
                &[JValue::from(data_size), JValue::from(crc32)],
            )
            .unwrap()
            .l()
            .unwrap();
        resource_id.into_inner()
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_getLinkHashNative(
        env: JNIEnv,
        _: JClass,
        jni_url: JString,
    ) -> jstring {
        let url_str: String = env.get_string(jni_url).expect("Failed to parse url").into();

        let url = Url::parse(url_str.as_str()).expect("Failed to parse url data");
        let link = Link::new(url, String::from(""), Some(String::from("")));

        env.new_string(link.id().unwrap().to_string())
            .expect("Couldn't create java string!")
            .into_inner()
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_loadLinkFileNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
        jni_file_path: JString,
    ) -> jobject {
        let link_data_cls = env.find_class("space/taran/arklib/LinkData").unwrap();
        let create_link_data_fn = env
            .get_static_method_id(
                link_data_cls,
                "create",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lspace/taran/arklib/LinkData;",
            )
            .unwrap();

        let file_path: String = env
            .get_string(jni_file_path)
            .expect("Failed to parse input file path")
            .into();

        let root_string: String = env
            .get_string(jni_root)
            .expect("Failed to parse root")
            .into();

        let path: &Path = Path::new(&file_path);
        let root: &Path = Path::new(&root_string);

        trace!("Received file path: {}", path.display());

        let link = Link::load(root, path).unwrap();

        let title = env
            .new_string(link.meta.title)
            .expect("Couldn't create java string!")
            .into_inner();

        let description = env
            .new_string(link.meta.desc.unwrap_or_default())
            .expect("Couldn't create java string!")
            .into_inner();

        let url = env
            .new_string(link.url)
            .expect("Couldn't create java string!")
            .into_inner();

        let image = env
            .new_string(String::from(""))
            .expect("Couldn't create java string!")
            .into_inner();

        let link_data = env
            .call_static_method_unchecked(
                link_data_cls,
                create_link_data_fn,
                JavaType::Object(String::from("space/taran/arklib/LinkData")),
                &[
                    JValue::from(title),
                    JValue::from(description),
                    JValue::from(url),
                    JValue::from(image),
                ],
            )
            .unwrap()
            .l()
            .unwrap();
        link_data.into_inner()
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_fetchLinkDataNative(
        env: JNIEnv,
        _: JClass,
        jni_url: JString,
    ) -> jobject {
        let url_str: String = env.get_string(jni_url).expect("Failed to parse url").into();

        let url = Url::parse(url_str.as_str()).expect("Failed to parse url data");

        trace!("Received url: {}", url.as_str());

        let og_result = Link::get_preview_synced(url);
        match og_result {
            Ok(og) => {
                trace!("Got link title: {}", og.title.to_owned().unwrap());
                let link_data_cls = env.find_class("space/taran/arklib/LinkData").unwrap();
                let create_link_data_fn = env
                    .get_static_method_id(
                        link_data_cls,
                        "create",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lspace/taran/arklib/LinkData;",
                    )
                    .unwrap();

                let title = env
                    .new_string(og.title.unwrap_or_default())
                    .expect("Couldn't create java string!")
                    .into_inner();
                let description = env
                    .new_string(og.description.unwrap_or_default())
                    .expect("Couldn't create java string!")
                    .into_inner();
                let url = env
                    .new_string(og.url.unwrap_or_default())
                    .expect("Couldn't create java string!")
                    .into_inner();
                let image = env
                    .new_string(og.image.unwrap_or_default())
                    .expect("Couldn't create java string!")
                    .into_inner();

                let link_data = env
                    .call_static_method_unchecked(
                        link_data_cls,
                        create_link_data_fn,
                        JavaType::Object(String::from("space/taran/arklib/LinkData")),
                        &[
                            JValue::from(title),
                            JValue::from(description),
                            JValue::from(url),
                            JValue::from(image),
                        ],
                    )
                    .unwrap()
                    .l()
                    .unwrap();
                link_data.into_inner()
            }
            Err(e) => {
                trace!("Fetch link preview: {:?}", e);
                JObject::null().into_inner()
            }
        }
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_createLinkFileNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
        jni_title: JString,
        jni_desc: JString,
        jni_url: JString,
        jni_base_path: JString,
        jni_download_preview: jboolean,
    ) {
        let title: String = env
            .get_string(jni_title)
            .expect("Failed to parse title")
            .into();

        let desc: String = env
            .get_string(jni_desc)
            .expect("Failed to parse description")
            .into();

        let base_path: String = env
            .get_string(jni_base_path)
            .expect("Failed to parse input base path")
            .into();

        let root_string: String = env
            .get_string(jni_root)
            .expect("Failed to parse root")
            .into();

        let path: &Path = Path::new(&base_path);
        let root: &Path = Path::new(&root_string);

        trace!("Received file path: {}", path.display());

        let url_str: String = env.get_string(jni_url).expect("Failed to parse url").into();

        let url = Url::parse(url_str.as_str()).expect("Failed to parse url data");

        trace!("Received url: {}", url.as_str());

        let download_preview = jni_download_preview != 0;

        let mut link = Link::new(url, title, Some(desc));
        let hashedLinkName = link.id().unwrap().to_string();
        let hashedLinkFileName = format!("{}.link", hashedLinkName);
        trace!("Generated hashed link filename: {}", hashedLinkFileName);
        link.write_to_path_sync(
            root,
            path.join(hashedLinkFileName).as_ref(),
            download_preview,
        )
        .unwrap();
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_LibKt_pdfPreviewGenerateNative(
        env: JNIEnv,
        _: JClass,
        jni_path: JString,
        jni_quality: JString,
    ) -> jobject {
        let file_path: String = env
            .get_string(jni_path)
            .expect("failed to parse input file name")
            .into();
        debug!("Preview PDF Path: {file_path}");
        let file = File::open(file_path).expect("failed to open file");
        let quality: String = env
            .get_string(jni_quality)
            .expect("failed to read quality")
            .into();

        let quality = match quality.as_str() {
            "HIGH" => PDFQuality::High,
            "MEDIUM" => PDFQuality::Medium,
            "LOW" => PDFQuality::Low,
            _ => {
                panic!("parameter illegal: {}", quality)
            }
        };

        let image = arklib::pdf::render_preview_page(file, quality);

        let bitmap_cls = env.find_class("android/graphics/Bitmap").unwrap();
        let create_bitmap_fn = env
            .get_static_method_id(
                bitmap_cls,
                "createBitmap",
                "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
            )
            .unwrap();

        let config_name = env.new_string("ARGB_8888").unwrap();
        let bitmap_config_cls = env.find_class("android/graphics/Bitmap$Config").unwrap();
        let value_of_bitmap_config_fn = env
            .get_static_method_id(
                bitmap_config_cls,
                "valueOf",
                "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;",
            )
            .unwrap();

        let bitmap_config = env
            .call_static_method_unchecked(
                bitmap_config_cls,
                value_of_bitmap_config_fn,
                JavaType::Object(String::from("android/graphics/Bitmap$Config")),
                &[JValue::from(config_name)],
            )
            .unwrap();

        let bitmap = env
            .call_static_method_unchecked(
                bitmap_cls,
                create_bitmap_fn,
                JavaType::Object(String::from("android/graphics/Bitmap")),
                &[
                    JValue::from(image.width() as jint),
                    JValue::from(image.height() as jint),
                    bitmap_config,
                ],
            )
            .unwrap()
            .l()
            .unwrap();

        let pixels = env
            .new_int_array((image.width() * image.height()) as jint)
            .unwrap();
        let rgba8img = image.as_rgba8().unwrap();

        let mut raw_pixel = vec![];
        for i in 0..((image.width() * image.height()) as usize) {
            let rgba8img = rgba8img.as_bytes();

            let red = rgba8img[i * 4] as i32;
            let green = rgba8img[i * 4 + 1] as i32;
            let blue = rgba8img[i * 4 + 2] as i32;
            let alpha = rgba8img[i * 4 + 3] as i32;
            let current_pixel = ((alpha.overflowing_shl(24)).0
                | (red.overflowing_shl(16)).0
                | (green.overflowing_shl(8)).0
                | (blue)) as i32;
            raw_pixel.push(current_pixel);
        }
        env.set_int_array_region(pixels, 0, raw_pixel.as_slice())
            .unwrap();
        let set_pixels_fn = env
            .get_method_id(bitmap_cls, "setPixels", "([IIIIIII)V")
            .unwrap();

        env.call_method_unchecked(
            bitmap,
            set_pixels_fn,
            JavaType::Primitive(Primitive::Void),
            &[
                JValue::from(pixels),
                JValue::from(0),
                JValue::from(rgba8img.width() as jint),
                JValue::from(0),
                JValue::from(0),
                JValue::from(rgba8img.width() as jint),
                JValue::from(rgba8img.height() as jint),
            ],
        )
        .unwrap();

        bitmap.into_inner()
    }

    static ROOT2INDEX: Lazy<Mutex<HashMap<Box<PathBuf>, ResourceIndex>>> = Lazy::new(|| {
        let m = HashMap::new();
        Mutex::new(m)
    });

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_binding_BindingIndex_loadNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
    ) -> jboolean {
        let root_string: String = env
            .get_string(jni_root)
            .unwrap()
            .into();

        let root: PathBuf = PathBuf::from(root_string);

        let loaded = match ResourceIndex::load(&root) {
            Ok(index) => {
                ROOT2INDEX.lock().unwrap().insert(Box::new(root), index);
                JNI_TRUE
            },
            Err(_) => JNI_FALSE
        };

        loaded
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_binding_BindingIndex_buildNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
    ) {
        let root_string: String = env
            .get_string(jni_root)
            .unwrap()
            .into();

        let root: PathBuf = PathBuf::from(root_string);

        let index = ResourceIndex::build(&root).unwrap();
        ROOT2INDEX.lock().unwrap().insert(Box::new(root), index);
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_binding_BindingIndex_updateNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
    ) -> jobject {
        let root_string: String = env
            .get_string(jni_root)
            .unwrap()
            .into();

        let root: PathBuf = PathBuf::from(root_string);

        let mut binding = ROOT2INDEX.lock().unwrap();
        let index = binding.get_mut(&root).unwrap();

        let index_update = index.update().unwrap();

        let jni_deleted_hashset = env.new_object("java/util/HashSet", "()V", &[]).unwrap();
        let jni_added_map = env.new_object("java/util/HashMap", "()V", &[]).unwrap();

        for id in &index_update.deleted {
            let id = env.new_string(id.to_string()).unwrap().into();

            env.call_method(
                jni_deleted_hashset,
                "add",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                &[id]
            );
        }

        for (path, id) in &index_update.added {
            let id = env.new_string(id.to_string()).unwrap().into();
            let path = env.new_string(path.to_str().unwrap()).unwrap().into();
            env.call_method(
                jni_added_map,
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                &[id, path]
            );
        }

        let jni_params_list = env.new_object("java/util/ArrayList", "()V", &[]).unwrap();
        env.call_method(
            jni_params_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[jni_deleted_hashset.into()]
        );
        env.call_method(
            jni_params_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[jni_added_map.into()]
        );
        
        jni_params_list.into_inner()
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_binding_BindingIndex_storeNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
    ) {
        let root_string: String = env
            .get_string(jni_root)
            .unwrap()
            .into();

        let root: PathBuf = PathBuf::from(root_string);

        let mut binding = ROOT2INDEX.lock().unwrap();
        let index = binding.get_mut(&root).unwrap();

        index.store();
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_binding_BindingIndex_id2PathNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
    ) -> jobject {
        let root_string: String = env
            .get_string(jni_root)
            .unwrap()
            .into();

        let root: PathBuf = PathBuf::from(root_string);

        let binding = ROOT2INDEX.lock().unwrap();
        let index = binding.get(&root).unwrap();

        let jni_map = env.new_object("java/util/HashMap", "()V", &[]).unwrap();

        for (id, path) in &index.id2path {
            let id = env.new_string(id.to_string()).unwrap().into();
            let path = env.new_string(path.to_str().unwrap()).unwrap().into();
            env.call_method(
                jni_map,
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                &[id, path]
            );
        }

        jni_map.into_inner()
    }

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arklib_binding_BindingIndex_path2idNative(
        env: JNIEnv,
        _: JClass,
        jni_root: JString,
    ) -> jobject {
        let root_string: String = env
            .get_string(jni_root)
            .unwrap()
            .into();

        let root: PathBuf = PathBuf::from(root_string);

        let binding = ROOT2INDEX.lock().unwrap();
        let index = binding.get(&root).unwrap();

        let jni_map = env.new_object("java/util/HashMap", "()V", &[]).unwrap();

        for (path, index_entry) in &index.path2id {
            let path = env.new_string(path.to_str().unwrap()).unwrap().into();
            let id = index_entry.id.to_string();
            let modified_millis = index_entry.modified.duration_since(UNIX_EPOCH).unwrap().as_millis().to_string();
            let id_to_modified = env.new_string(id + ":" + &modified_millis).unwrap().into();
            env.call_method(
                jni_map,
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                &[path, id_to_modified]
            );
        }

        jni_map.into_inner()
    }
}
