{{/* Short chart name, with an optional override for reuse in another chart. */}}
{{- define "interview-lab.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}
{{/* Release-qualified name; Kubernetes resource names have a 63-character limit. */}}
{{- define "interview-lab.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "interview-lab.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{/* Shared labels connect chart objects to the Helm release and app version. */}}
{{- define "interview-lab.labels" -}}
app.kubernetes.io/name: {{ include "interview-lab.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
