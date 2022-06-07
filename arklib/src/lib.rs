#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use jni::objects::{JClass, JString};
    use jni::sys::jlong;
    use jni::sys::{jbyteArray, jstring};
    use jni::JNIEnv;
    use log::{debug, error, log, log_enabled, trace, Level};
    use std::path::Path;

    extern crate android_logger;
    use android_logger::Config;

    #[no_mangle]
    pub extern "C" fn Java_space_taran_arknavigator_mvp_model_repo_index_ResourceIdKt_computeIdNative(
        env: JNIEnv,
        _: JClass,
        jni_size: i64,
        jni_file_name: JString,
    ) -> jlong {
        android_logger::init_once(Config::default().with_min_level(Level::Trace));

        let file_size: usize =
            usize::try_from(jni_size).expect(&format!("Failed to parse input size"));
        println!("Received size: {}", file_size);
        let file_name: String = env
            .get_string(jni_file_name)
            .expect("Failed to parse input file name")
            .into();
        let file_path: &Path = Path::new(&file_name);
        trace!("Received filename: {}", file_path.display());

        arklib::id::ResourceId::compute(file_size.try_into().unwrap(), file_path)
            .crc32
            .into()
    }
    #[no_mangle]
    pub extern "C" fn Java_space_taran_arknavigator_native_LibKt_pdfThumbnailGenerate(
        env: JNIEnv,
        _: JClass,
        jni_img_data: jbyteArray,
        jni_font_path: JString,
    ) -> jbyteArray {
        android_logger::init_once(
            Config::default()
                .with_min_level(Level::Trace)
                .with_tag("arklib"),
        );

        let data: Vec<u8> = env
            .convert_byte_array(jni_img_data)
            .expect("failed to read bytes");
        let fontpath: String = env
            .get_string(jni_font_path)
            .expect("cannot get font path")
            .into();
        debug!("Received: {}", data.len());
        // let data = arklib::pdf::render_preview_page(
        //     data.as_slice(),
        //     arklib::pdf::PDFQuailty::Low,
        //     Some("/data/user/0/space.taran.arknavigator/cache/fonts".into()),
        // );
        // });
        // let output = env.byte_array_from_slice(data.as_raw().as_slice()).unwrap();
        // println!("data!");
        jni_img_data
    }
}
