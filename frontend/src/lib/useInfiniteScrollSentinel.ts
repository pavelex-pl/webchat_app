import { useEffect, useRef } from "react";

export function useInfiniteScrollSentinel<T extends HTMLElement>(
  hasNextPage: boolean | undefined,
  isFetchingNextPage: boolean,
  fetchNextPage: () => void,
) {
  const ref = useRef<T | null>(null);
  const cbRef = useRef(fetchNextPage);
  useEffect(() => {
    cbRef.current = fetchNextPage;
  }, [fetchNextPage]);
  useEffect(() => {
    const el = ref.current;
    if (!el || !hasNextPage || isFetchingNextPage) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) cbRef.current();
      },
      { rootMargin: "120px" },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [hasNextPage, isFetchingNextPage]);
  return ref;
}
