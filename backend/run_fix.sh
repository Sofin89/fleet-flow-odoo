#!/bin/bash

# Script to fix driver email in the database
# This updates the driver email to match the user email

echo "Connecting to Neon database and fixing driver email..."

PGPASSWORD='npg_y4I3oGnubTSJ' psql \
  -h ep-wandering-wildflower-a1exphgy-pooler.ap-southeast-1.aws.neon.tech \
  -U neondb_owner \
  -d neondb \
  -c "UPDATE drivers SET email = 'driver@fleetflow.com' WHERE email = 'vikram@fleetflow.com';"

echo ""
echo "Verifying the update..."

PGPASSWORD='npg_y4I3oGnubTSJ' psql \
  -h ep-wandering-wildflower-a1exphgy-pooler.ap-southeast-1.aws.neon.tech \
  -U neondb_owner \
  -d neondb \
  -c "SELECT id, full_name, email, license_number FROM drivers WHERE email = 'driver@fleetflow.com';"

echo ""
echo "Done! The driver email has been updated."
