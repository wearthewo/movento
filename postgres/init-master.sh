#!/bin/bash
set -e

# Create replication user
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_password';
    SELECT pg_create_physical_replication_slot('replication_slot');
EOSQL

# Allow replication connections
echo "host replication replicator 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
