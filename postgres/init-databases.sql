SELECT 'CREATE DATABASE movento_user_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='movento_user_db')\gexec
SELECT 'CREATE DATABASE movento_content_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='movento_content_db')\gexec
SELECT 'CREATE DATABASE movento_streaming_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='movento_streaming_db')\gexec
SELECT 'CREATE DATABASE movento_payment_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='movento_payment_db')\gexec
SELECT 'CREATE DATABASE movento_recommendation_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='movento_recommendation_db')\gexec
