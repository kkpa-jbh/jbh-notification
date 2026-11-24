-- The real database gets this from jbh-deploy/initdb (outside Liquibase). Liquibase needs it first.
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
