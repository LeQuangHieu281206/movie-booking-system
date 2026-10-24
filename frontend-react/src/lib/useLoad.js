import { useEffect, useState } from "react";

export function useLoad(loader, dependencies) {
  const [state, setState] = useState({ data: null, loading: true, error: "" });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    setState({ data: null, loading: true, error: "" });
    Promise.resolve()
      .then(() => loader({ signal: controller.signal }))
      .then((data) => {
        if (active) setState({ data, loading: false, error: "" });
      })
      .catch((error) => {
        if (active && error.name !== "AbortError")
          setState({ data: null, loading: false, error: error.message });
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [...dependencies, revision]);
  return { ...state, reload: () => setRevision((value) => value + 1) };
}
