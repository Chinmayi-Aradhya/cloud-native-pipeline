{{- define "consumer.name" -}}
{{ .Chart.Name }}
{{- end -}}

{{- define "consumer.fullname" -}}
{{ .Release.Name }}-{{ .Chart.Name }}
{{- end -}}