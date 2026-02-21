-- Fix driver email to match user email
-- This allows the driver user to log in and access their profile

UPDATE drivers 
SET email = 'driver@fleetflow.com' 
WHERE email = 'vikram@fleetflow.com';

-- Verify the update
SELECT id, full_name, email, license_number 
FROM drivers 
WHERE email = 'driver@fleetflow.com';
