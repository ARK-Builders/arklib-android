// Modify From https://github.com/kawamuray/wasmtime-java
use crate::errors::Result;
use jni::descriptors::Desc;
use jni::errors::{Error, Result as JniResult};
use jni::objects::{JFieldID, JObject};
use jni::signature::{JavaType, Primitive};
use jni::strings::JNIString;
use jni::sys::jlong;
use jni::JNIEnv;
use std::sync::Mutex;

pub const INNER_PTR_FIELD: &str = "innerPtr";

/// Surrender a Rust object into a pointer.
/// The given value gets "forgotten" by Rust's memory management
/// so you have to get it back into a `T` at some point to avoid leaking memory.
pub fn into_raw<T>(val: T) -> jlong
where
    T: 'static,
{
    Box::into_raw(Box::new(Mutex::new(val))) as jlong
}

/// Restore a Rust object of type `T` from a pointer.
/// This is the reverse operation of `into_raw`.
pub fn from_raw<T>(ptr: jlong) -> Result<T> {
    Ok((*unsafe { Box::from_raw(ptr as *mut Mutex<T>) }).into_inner()?)
}
