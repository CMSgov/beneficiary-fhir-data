# Builds complete image spec by combining imageName and bfdVersion.
{{- define "imageSpec" -}}
    {{- printf "%s%s:%s" .Values.imageRegistry .Values.imageName .Values.bfdVersion -}}
{{- end -}}

# Builds LayeredConfig configuration string with all ssmHierarchies.
{{- define "layeredConfigJson" -}}
{
    "ssmHierarchies": [
        {{- range $i, $path := (.Values.ssmHierarchies) }}
        {{ gt $i 0 | ternary "," "" }}{{ $path | quote }}
        {{- end }}
    ]
}
{{- end -}}
