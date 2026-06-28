import type { AnyFieldApi } from '@tanstack/react-form';

export function FieldInfo({ field }: { field: AnyFieldApi }) {
  if (field.state.meta.isTouched && !field.state.meta.isValid) {
    return field.state.meta.errors.map((error) => error?.message ?? error).join(", ");
  }

  if (field.state.meta.isValidating) {
    return "Validating...";
  }

  return null;
}
