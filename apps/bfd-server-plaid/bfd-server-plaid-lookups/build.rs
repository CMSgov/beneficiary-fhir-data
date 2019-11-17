use bfd_server_plaid_lookups_codegen::ndc_codegen;

/// Entry point for this `build.rs` script, which Cargo will call before compiling the rest of the
/// code (in `src/`).
fn main() -> std::io::Result<()> {
    ndc_codegen::generate_ndc_descriptions()
}
