#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use jni::objects::{JClass, JString};
    use jni::sys::jbyteArray;
    use jni::sys::jlong;
    use jni::JNIEnv;

    use log::{trace, Level};
    use std::path::Path;

    extern crate android_logger;
    use android_logger::Config;

    #[no_mangle]
    pub unsafe extern "C" fn Java_space_taran_arknavigator_mvp_model_repo_index_ResourceIdKt_computeIdNative(
        env: JNIEnv,
        _: JClass,
        jni_size: i64,
        jni_file_name: JString,
    ) -> jlong {
        android_logger::init_once(Config::default().with_min_level(Level::Trace));
        let file_size: usize =
            usize::try_from(jni_size).expect(&format!("Failed to parse input size"));
        trace!("Received size: {}", file_size);
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
    pub fn Java_space_taran_arklib_LibKt_pdfThumbnailGenerate(
        env: JNIEnv,
        _: JClass,
        jni_img_data: jbyteArray,
    ) -> jbyteArray {
        let data: Vec<u8> = env.convert_byte_array(jni_img_data).unwrap();
        let data = std::thread::spawn(move || {
            arklib::pdf::render_preview_page(data.as_slice(), arklib::pdf::PDFQuailty::Low)
        });
        let output = env
            .byte_array_from_slice(data.join().unwrap().as_raw().as_slice())
            .unwrap();
        output
    }
}
