#!/bin/bash
source .env

if [ -z "$1" ]; then
    SPEC_FILE="$PATH_TO_ABS_PROJECT/docs/openapi.json"
else
    SPEC_FILE="$1"
fi

API_NAME="api.abs.generated" # Name for your API

# Output directory for generated code
TEMP_OUTPUT_DIR="generatedapi"
OUTPUT_DIR="../../../backup-tests/src/main/java/api/abs/generated"

# Check if openapi-generator is installed
if ! command -v openapi-generator &> /dev/null
then
    echo "openapi-generator is not installed. Installing now via Homebrew..."
    brew install openapi-generator
else
    # Silently upgrade if a newer version is available
    brew upgrade openapi-generator --quiet
fi

# Run the OpenAPI generator command
openapi-generator generate \
    -i "$SPEC_FILE" \
    -g "java" \
    -o "$TEMP_OUTPUT_DIR" \
    --api-package "$API_NAME.api" \
    --model-package "$API_NAME.model" \
    --additional-properties=library=okhttp-gson,useRuntimeException=true

echo "Code generation complete."

rm -rf $OUTPUT_DIR
mv $TEMP_OUTPUT_DIR/src/main/java/api/abs/generated $OUTPUT_DIR

find "$OUTPUT_DIR" -type f -name "*.java" | while read -r file; do
    # Use sed to remove lines starting with "@javax.annotation.Generated"
    sed -i '' '/^@javax\.annotation\.Generated/d' "$file"
done

rm -rf $TEMP_OUTPUT_DIR