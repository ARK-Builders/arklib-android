#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use jni::JNIEnv;
    use jni::objects::{JString, JClass};
    use jni::sys::{jlong};

    use std::path::Path;
    use log::{Level, trace};

    extern crate android_logger;
    use android_logger::Config;

    #[no_mangle]
    pub unsafe extern fn Java_space_taran_arknavigator_mvp_model_repo_index_ResourceIdKt_computeIdNative(
        env: JNIEnv,
        _: JClass,
        jni_size: i64,
        jni_file_name: JString) -> jlong {
        android_logger::init_once(
            Config::default().with_min_level(Level::Trace));
        let file_size: usize = usize::try_from(jni_size)
            .expect(&format!("Failed to parse input size"));
        trace!("Received size: {}", file_size);
        let file_name: String = env
            .get_string(jni_file_name)
            .expect("Failed to parse input file name")
            .into();
        let file_path: &Path = Path::new(&file_name);
        trace!("Received filename: {}", file_path.display());
        return arklib::resource_id::compute_id(file_size, file_path);
    }
}
