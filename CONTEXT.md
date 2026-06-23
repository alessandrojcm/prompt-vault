# Prompt Vault

Prompt Vault is a web application for creating and sharing LLM prompts with keyword-triggered security alerts.

## Language

**User**:
The single authenticated identity in Prompt Vault. A user has exactly one role, such as admin or normal user.
_Avoid_: Account, member

**Admin**:
A user whose role grants administrative capabilities. Admins are predefined by seeding them into the system rather than being created through signup.
_Avoid_: Administrator account

**Normal User**:
A user whose role represents regular application access. Normal users are created through signup.
_Avoid_: Regular user, standard user

**Role**:
The fixed classification that determines whether a user is an admin or a normal user. Roles are assigned when a user is created and are not changed through the application.
_Avoid_: User type, permission level

**Username**:
The unique name a user uses to log in and be identified in Prompt Vault.
_Avoid_: Handle, screen name

**Email Address**:
The unique contact address associated with a user.
_Avoid_: Email, contact email

**Account Status**:
Whether a user is allowed to authenticate. Enabled users can log in; disabled users cannot log in.
_Avoid_: State, active/inactive

**Current User**:
The user associated with the active authenticated session.
_Avoid_: Profile, me
