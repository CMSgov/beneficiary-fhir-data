#!/bin/bash
echo "Installing development tools with UV..."

for tool in ruff pytest pyright black mypy; do
    echo "Installing $tool..."
    uv tool install $tool
done

echo ""
echo "✅ Tools installed:"
ruff --version
pytest --version
pyright --version
