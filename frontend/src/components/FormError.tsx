export default function FormError({ error }: { error?: string | null }) {
  if (!error) return null;
  return <div className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</div>;
}
