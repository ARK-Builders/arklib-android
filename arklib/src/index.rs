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

use crate::interop;

struct RustResourcesIndex;

impl RustResourcesIndex {
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_init(env: &JNIEnv, root_path: JString, res: JObject) -> jlong {
        let cls = env.get_object_class(res).unwrap();
        let root_path = env.get_string(root_path).unwrap().into();

        let ri = ResourceIndex::new(root_path, res);
        interop::into_raw::<ResourceIndex>(ri);
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_listResources(
        env: JNIEnv,
        clazz: JClass,
        prefix: JString,
    ) -> Vec<jlong> {
        let prefix: String = env.get_string(prefix).unwrap().into();
        let path = PathBuf::from(prefix);
        todo!()
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_getPath(id: jlong) -> String {
        todo!()
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_getMeta(id: jlong) {}
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_reindex(env: &JNIEnv, this: JObject) {
        let ri = interop::get_inner::<ResourceIndex>(env, this).unwrap();
        ri.update();
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_remove(env: &JNIEnv, id: jlong, this: JObject) {
        let ri = interop::get_inner::<ResourceIndex>(env, this).unwrap();
        ri.path2meta.retain(|_, v| id == v.id.crc32.into());
    }
    #[jni_fn("space.taran.arklib.index")]
    pub fn RustResourcesIndex_updateResource() {}
}
