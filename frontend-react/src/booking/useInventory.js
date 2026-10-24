import { useCallback, useEffect, useRef, useState } from "react";
import { readInventory } from "./flow.js";

export function useInventory(api, showtimeId, paused) {
  const [state, setState] = useState({
    data: null,
    loading: true,
    refreshing: false,
    error: "",
  });
  const serial = useRef(0),
    controller = useRef(null),
    mounted = useRef(false),
    pause = useRef(paused);
  pause.current = paused;
  const refresh = useCallback(
    async (options = {}) => {
      if (pause.current && options.force !== true) return null;
      controller.current?.abort();
      const current = ++serial.current,
        request = new AbortController();
      controller.current = request;
      setState((previous) => ({
        ...previous,
        loading: !previous.data,
        refreshing: Boolean(previous.data),
        error: "",
      }));
      try {
        const data = await readInventory(api, showtimeId, {
          signal: request.signal,
        });
        if (
          mounted.current &&
          serial.current === current &&
          !request.signal.aborted
        )
          setState({ data, loading: false, refreshing: false, error: "" });
        return request.signal.aborted ? null : data;
      } catch (error) {
        if (
          mounted.current &&
          serial.current === current &&
          !request.signal.aborted
        )
          setState((previous) => ({
            ...previous,
            loading: false,
            refreshing: false,
            error: error.message,
          }));
        return null;
      }
    },
    [api, showtimeId],
  );
  useEffect(() => {
    mounted.current = true;
    refresh();
    return () => {
      mounted.current = false;
      serial.current++;
      controller.current?.abort();
    };
  }, [refresh]);
  useEffect(() => {
    if (paused) {
      controller.current?.abort();
      return;
    }
    const timer = setInterval(refresh, 30000);
    const focus = () => {
      if (document.visibilityState === "visible") refresh();
    };
    window.addEventListener("focus", focus);
    return () => {
      clearInterval(timer);
      window.removeEventListener("focus", focus);
    };
  }, [refresh, paused]);
  function adopt(data) {
    serial.current++;
    controller.current?.abort();
    setState({ data, loading: false, refreshing: false, error: "" });
  }
  return { ...state, refresh, adopt };
}
