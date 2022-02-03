INSERT INTO mbi_sets (set_type, mbi)
VALUES
{{#SQLValues this 1}}('Dev', '{{this}}'){{#if @last}};{{else}},{{/if}}
{{/SQLValues}}

INSERT INTO mbi_sets (set_type, mbi)
VALUES
{{#SQLValues this 2}}('Small', '{{this}}'){{#if @last}};{{else}},{{/if}}
{{/SQLValues}}

INSERT INTO mbi_sets (set_type, mbi)
VALUES
{{#SQLValues this 4}}('Large', '{{this}}'){{#if @last}};{{else}},{{/if}}
{{/SQLValues}}