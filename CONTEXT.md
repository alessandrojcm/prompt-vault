# Prompt Vault

Prompt Vault is a web application for creating and sharing LLM prompts with keyword-triggered security alerts.

## Language

**User**:
The single authenticated identity in Prompt Vault. A user has exactly one role, such as admin or normal user.
_Avoid_: Account, member

**Admin**:
A user whose role grants administrative capabilities. Admins are predefined by seeding them into the system rather than being created through signup, and can manage the account status of normal users but not admins.
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
Whether a user is allowed to authenticate. Enabled users can log in; disabled users cannot log in or continue using an existing authenticated session.
_Avoid_: State, active/inactive

**Current User**:
The user associated with the active authenticated session.
_Avoid_: Profile, me

**User Management**:
The admin capability for viewing registered users and managing the account status of normal users.
_Avoid_: User admin, account management

**Prompt**:
A saved, user-owned prompt record in Prompt Vault. A prompt contains prompt text and metadata such as its title, category, and visibility.
_Avoid_: Saved prompt, prompt entry

**Prompt Text**:
The LLM instruction or content stored inside a prompt. Prompt text is stored with surrounding whitespace trimmed while preserving internal whitespace and newlines.
_Avoid_: Body, content

**Prompt Title**:
The display label for a prompt. Prompt titles are stored with surrounding whitespace trimmed and are not unique identifiers.
_Avoid_: Name

**Prompt Visibility**:
Whether a prompt is private to its owner or public to all enabled authenticated users. New prompts start private; public prompts owned by disabled users are not visible to others.
_Avoid_: Shared status, access level

**Prompt Owner**:
The user who created a prompt and is the only user allowed to change or delete it. Public prompts identify their owner by username; admins do not gain ownership privileges over prompts created by other users.
_Avoid_: Author, creator

**Prompt Category**:
A system-wide classification that every prompt belongs to exactly once. Prompts can only use an existing prompt category; categories have a unique user-facing label and unique auto-generated slug derived from the label, and are not owned resources.
Initial categories are Coding, Research, Cybersecurity, HR, Legal, and Personal Productivity.
Categories referenced by prompts remain available until those prompts are moved to another category.
_Avoid_: Tag, topic

**Share**:
The user action of making one of their prompts public.
_Avoid_: Publish

**Unshare**:
The user action of making one of their public prompts private again.
_Avoid_: Unpublish

**My Prompts**:
The collection of prompts owned by the current user, regardless of visibility.
_Avoid_: Owned prompts

**Public Prompts**:
The collection of public prompts owned by other enabled users and visible to the current user.
_Avoid_: Prompt feed, shared prompts

**Policy Keyword**:
A keyword or phrase that admins manage to identify Prompt Text that may require review. Policy keywords are matched against Prompt Text when prompt text is created or changed.
_Avoid_: Forbidden word, blocked term

**Prompt Flag**:
A durable record that a Prompt matched one or more Policy Keywords. A Prompt Flag captures snapshots of the matched keyword text as it existed when the Prompt Text was checked and when the current Prompt Text was flagged.
_Avoid_: Alert, violation, moderation finding

**Flagged Prompt**:
A Prompt that has an associated Prompt Flag.
_Avoid_: Flagged content, reported prompt
