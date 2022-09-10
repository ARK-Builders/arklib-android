use anyhow;
use jni::descriptors::Desc;
use jni::objects::JThrowable;
use jni::{self, JNIEnv};
use std::io;
use thiserror::Error;

pub type Result<T, E = Error> = std::result::Result<T, E>;

#[derive(Debug, Error)]
pub enum Error {
    #[error("JNI error: {0}")]
    Jni(#[from] jni::errors::Error),
    #[error("{0}")]
    LockPoison(String),
}

impl<G> From<std::sync::PoisonError<G>> for Error {
    fn from(err: std::sync::PoisonError<G>) -> Self {
        Error::LockPoison(err.to_string())
    }
}

impl<'a> Desc<'a, JThrowable<'a>> for Error {
    fn lookup(self, env: &JNIEnv<'a>) -> jni::errors::Result<JThrowable<'a>> {
        use Error::*;
        let (ex_class, msg) = match &self {
            Jni(e) => {
                use jni::errors::Error::*;
                match e {
                    JavaException => return env.exception_occurred(),
                    NullPtr(_) | NullDeref(_) => {
                        ("java/lang/NullPointerException", self.to_string())
                    }
                    _ => (
                        "java/lang/RuntimeException",
                        format!("unknown exception caught (likely a BUG): {}", self),
                    ),
                }
            }
            LockPoison(_) => ("java/lang/RuntimeException", self.to_string()),
        };

        let jmsg = env.new_string(msg)?;
        Ok(env
            .new_object(ex_class, "(Ljava/lang/String;)V", &[jmsg.into()])?
            .into())
    }
}
