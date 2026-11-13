# Trusted epilogue helper. Only executed inside the judge sandbox.
import inspect as apH_inspect
import sys as apH_sys


def apH_signature_error():
    apH_sys.stderr.write("@@APH-HARNESS-SIGNATURE-ERROR@@\n")
    raise SystemExit(92)


def apH_resolve_call(apH_name, apH_args):
    apH_missing = object()
    apH_solution = globals().get("Solution", apH_missing)
    if apH_solution is apH_missing or not callable(apH_solution):
        apH_signature_error()
    # Constructor exceptions retain their runtime classification.
    apH_instance = apH_solution()
    apH_static = apH_inspect.getattr_static(apH_instance, apH_name, apH_missing)
    try:
        apH_callable = getattr(apH_instance, apH_name)
    except AttributeError:
        if apH_static is apH_missing:
            apH_signature_error()
        raise
    if not callable(apH_callable):
        apH_signature_error()
    try:
        apH_signature = apH_inspect.signature(apH_callable)
    except (TypeError, ValueError):
        # Unsupported introspection does not prove that the callable is invalid.
        return apH_callable
    try:
        apH_signature.bind(*apH_args)
    except TypeError:
        apH_signature_error()
    # The actual invocation belongs outside this guard; body exceptions remain runtime errors.
    return apH_callable
