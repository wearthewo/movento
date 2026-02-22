#!/bin/bash
set -e

# Stop PostgreSQL to allow us to set up replication
pg_ctl -D "$PGDATA" -m fast -w stop

# Remove any existing data and set up as a replica
rm -rf "$PGDATA"/*

# Take a base backup from the master
PGPASSWORD=replicator_password pg_basebackup -h postgres-master -U replicator -D "$PGDATA" -P --slot=replication_slot --write-recovery-conf

# Configure the replica
cat >> "$PGDATA/postgresql.conf" << EOF
hot_standby = on
hot_standby_feedback = on
max_standby_streaming_delay = 30s
max_standby_archive_delay = 30s
EOF

# Start PostgreSQL in the background
pg_ctl -D "$PGDATA" -o "-c listen_addresses='*'" -w start

# Create additional users/databases if needed
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Add any additional users or permissions here
    ALTER SYSTEM SET hot_standby = 'on';
EOSQL

# Stop PostgreSQL to let the main container start it
pg_ctl -D "$PGDATA" -m fast -w stop
