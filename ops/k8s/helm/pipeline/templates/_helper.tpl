# Builds base name for all resources based on appName and pipelineType.
{{- define "pipelineName" -}}
    {{- if .Values.pipelineType -}}
        {{- printf "%s-%s" .Values.appName .Values.pipelineType -}}
    {{- else -}}
        {{- .Values.appName -}}
    {{- end -}}
{{- end -}}

# Builds complete image spec by combining imageName and bfdVersion.
{{- define "imageSpec" -}}
    {{- printf "%s:%s" .Values.imageName .Values.bfdVersion -}}
{{- end -}}
