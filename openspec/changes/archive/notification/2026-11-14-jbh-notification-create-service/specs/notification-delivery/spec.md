## ADDED Requirements

### Requirement: Notification language
The service SHALL choose the language of a notification from the request `locale`. If the
request has no `locale`, the service SHALL use `es`. The service SHALL NOT read user
preferences.

#### Scenario: Request locale wins
- **GIVEN** a send request with metadata `locale` = `en` and a `recipientId`
- **WHEN** the notification is sent
- **THEN** the saved notification has `locale` = `en`
- **AND** the subject and body use the English texts

#### Scenario: Missing locale falls back to Spanish
- **GIVEN** a send request with no `locale` in its metadata
- **WHEN** the notification is sent
- **THEN** the saved notification has `locale` = `es`
- **AND** this holds whether `recipientId` is set or not

#### Scenario: No call to another module
- **GIVEN** the service is built
- **WHEN** its sources and dependencies are checked
- **THEN** no class imports `com.jbh.preferences`
- **AND** no pom depends on `jbh-preferences-contracts`

### Requirement: Existing notification behavior is kept
The service SHALL keep the behavior it has in `jbh-personal-finance`: validation, the
REST API, the saved data and the email templates.

#### Scenario: Same data store
- **GIVEN** the service starts with the default `DATABASE_URL`
- **WHEN** it connects
- **THEN** it uses database `jbh_finance`, schema `notifications`
- **AND** it creates or changes no database object

#### Scenario: Migration history is kept
- **GIVEN** schema `notifications` was migrated by `jbh-personal-finance`
- **WHEN** this service starts
- **THEN** Liquibase skips the changeset already applied
- **AND** the start does not fail
