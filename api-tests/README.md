# API Test Scripts

This directory contains curl scripts to test the Hooked API endpoints.

## Prerequisites

1. Make sure the Phoenix server is running:
   ```bash
   cd ../backend
   mix phx.server
   ```

2. Server should be accessible at `http://localhost:4000`

## Usage

Make all scripts executable:
```bash
chmod +x *.sh
```

### 1. List All Catches
```bash
./01-list-catches.sh
```

### 2. Get Specific Catch
```bash
./02-get-catch.sh <catch_id>
```
Example:
```bash
./02-get-catch.sh 123e4567-e89b-12d3-a456-426614174000
```

### 3. Create Catch Without Image
```bash
./03-create-catch-no-image.sh
```

### 4. Create Catch With Image
```bash
./04-create-catch-with-image.sh /path/to/your/image.jpg
```
Example:
```bash
./04-create-catch-with-image.sh ~/Pictures/fish.jpg
```

### 5. Update Catch
```bash
./05-update-catch.sh <catch_id>
```

### 6. Delete Catch
```bash
./06-delete-catch.sh <catch_id>
```

## API Details

### Required Fields
- `species` (string, 1-100 chars)
- `location` (string, 1-200 chars)
- `caught_at` (datetime in ISO format)

### Optional Fields
- `latitude` (-90 to 90)
- `longitude` (-180 to 180)
- `notes` (max 1000 chars)
- `weather_data` (map - usually auto-populated)

### Image Constraints
- Supported formats: JPEG, PNG, WebP, HEIC
- Max size: 10MB
- Extensions: .jpg, .jpeg, .png, .webp, .heic

## Testing Workflow

1. Start with listing catches: `./01-list-catches.sh`
2. Create a catch: `./03-create-catch-no-image.sh`
3. Note the returned ID and get the specific catch: `./02-get-catch.sh <id>`
4. Update the catch: `./05-update-catch.sh <id>`
5. Create a catch with image: `./04-create-catch-with-image.sh /path/to/image.jpg`
6. Clean up by deleting catches: `./06-delete-catch.sh <id>`