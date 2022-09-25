#[macro_export]
macro_rules! wrap_error {
    ($env:expr, $body:expr) => {
        match $body {
            Ok(v) => v,
            Err(e) => {
                log::error!("{e}");
                panic!()
            }
        }
    };
}
