use std::path::{Path, PathBuf};

use arklib::index::{IndexUpdate, ResourceIndex};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::signature::JavaType;
use jni::sys::jlong;
use jni::sys::jobject;
use jni::{objects::AutoLocal, JNIEnv};
use jni_fn::jni_fn;
use paste::paste;
use robusta_jni::convert::{Signature, TryFromJavaValue};

struct RustResourcesIndex {
    inner: ResourceIndex,
}

impl RustResourcesIndex {
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_listResources(env: JNIEnv, jni_prefix: JString) -> Vec<jlong> {
        let prefix: String = env.get_string(jni_prefix).unwrap().into();
        let path = PathBuf::from(prefix);
        todo!()
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_getPath(id: jlong) -> String {
        todo!()
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_getMeta(&mut self, id: jlong) {}
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_reindex() {}
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_remove(&mut self, id: jlong) {
        self.inner.path2meta.retain(|_, v| id == v.id.crc32.into());
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_updateResource() {}
}
