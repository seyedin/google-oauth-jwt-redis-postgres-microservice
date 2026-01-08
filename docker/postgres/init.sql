-- Create application user
CREATE USER auth_user WITH PASSWORD 'auth_pass';

-- Create application database owned by auth_user
CREATE DATABASE auth_db OWNER auth_user;

-- Grant privileges on the database
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;

-- Connect to the new database to set schema privileges
\connect auth_db

-- Grant privileges on public schema to auth_user
GRANT ALL ON SCHEMA public TO auth_user;
