# Struct Update Guidelines

## 🎯 Project-Wide Consistency Rules

### ✅ **Use Struct Update Syntax When:**
- Updating **known fields** on **Ecto schemas** or **defined structs**
- Field exists at compile time
- Working with typed data

```elixir
# ✅ Good - Known struct fields
%{user_catch | species: "Largemouth Bass"}
%{user | name: "John", email: "john@example.com"}
```

### ✅ **Use Map Functions When:**
- Working with **dynamic/unknown keys**
- Building **plain maps** (not structs)
- **Merging** multiple maps
- **Runtime-determined** keys

```elixir
# ✅ Good - Dynamic keys
Map.put(dynamic_map, runtime_key, value)
Map.merge(attrs, image_data)
Enum.reduce(data, %{}, fn {k, v}, acc -> Map.put(acc, k, v) end)
```

## 📋 **Current Project Status**

### **Compliant Files:**
- ✅ `species_enricher.ex` - Uses struct update for UserCatch
- ✅ `catch_enrichment_worker.ex` - Uses Map.put for dynamic merging
- ✅ `catches.ex` - Uses Map.merge for attrs combining

### **Files to Review:**
- 🔍 `geo_enricher.ex` - Uses Map.get, could use pattern matching
- 🔍 `weather_enricher.ex` - Minimal implementation

## 🔧 **Enforcement Strategy**

### **1. Credo Rules**
Add to `.credo.exs`:
```elixir
{Credo.Check.Refactor.MapInto, []},
{Credo.Check.Warning.MapGetUnsafePass, []}
```

### **2. Code Review Checklist**
- [ ] Struct updates use `%{struct | field: value}` syntax
- [ ] Dynamic maps use `Map.*` functions
- [ ] Pattern matching preferred over `Map.get` when possible

### **3. Examples**

#### ✅ **Good Patterns:**
```elixir
# Struct updates
%{user_catch | species: species, confidence: 0.95}

# Dynamic map building
enriched_data = Enum.reduce(changes, %{}, fn {k, v}, acc ->
  Map.put(acc, to_string(k), v)
end)

# Map merging
Map.merge(base_attrs, additional_data)
```

#### ❌ **Avoid:**
```elixir
# Don't use Map.put for known struct fields
Map.put(user_catch, :species, species)

# Don't use struct update for dynamic keys
%{map | runtime_key => value}  # Compile error anyway
```

## 🎯 **Action Items**

1. **Immediate**: All new code follows these guidelines
2. **Next Sprint**: Review and update existing enrichers
3. **Future**: Add Credo rules for enforcement