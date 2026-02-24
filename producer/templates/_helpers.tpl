{{- define "producer.name" -}}
{{ .Chart.Name }}
{{- end -}}

{{- define "producer.fullname" -}}
{{ .Release.Name }}-{{ .Chart.Name }}
{{- end -}}