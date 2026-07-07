export function backendUrl(): string {
  const protocol = window.location.protocol;
  const host = window.location.host;
  return protocol + '//' + host + '/';
}

export function formatDateForApi(date: Date): string {
  const iso = date.toISOString();
  return iso.replace('Z', '+0000');
}

export function parseApiDate(dateStr: string): string {
  return dateStr.substring(0, 10);
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}
