# Seed Admins and Keep Roles Immutable

Prompt Vault creates admins only through database seeding, while public signup always creates normal users. User roles are assigned at creation and are not changed through the application, keeping admin creation and role governance outside the user-facing flows.
