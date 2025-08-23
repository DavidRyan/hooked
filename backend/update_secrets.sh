#!/bin/bash

# Update dev.exs - secret_key_base
sed -i '' 's/secret_key_base: "your-secret-key-base-here",/secret_key_base: System.get_env("SECRET_KEY_BASE") || raise("SECRET_KEY_BASE not set"),/' config/dev.exs

# Update dev.exs - JWT secret
sed -i '' 's/"lSecx5L\/Nqhs1Lp4XezmvOA+xgpTBB0Hn9W4aT7EnRXfPswG\/8wdvql9uwQROsze"/System.get_env("JWT_SECRET") || raise("JWT_SECRET not set")/' config/dev.exs

# Update endpoint.ex - signing salt
sed -i '' 's/signing_salt: "6fw2xNSWL2IsJFqBDDA0zsgxajmfk0b2JnrxX0uMhhXRZhfB0GcnBeOYdHZwDAz7",/signing_salt: System.get_env("SIGNING_SALT") || "dev-fallback",/' lib/hooked_api/endpoint.ex

echo "✅ Updated all secret references to use environment variables"
echo "🔑 Don't forget to source your .env file or set environment variables"
