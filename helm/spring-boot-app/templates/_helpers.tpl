{{- define "spring-boot-app.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "spring-boot-app.labels" -}}
app: {{ include "spring-boot-app.name" . }}
chart: {{ .Chart.Name }}-{{ .Chart.Version }}
release: {{ .Release.Name }}
{{- end -}}
