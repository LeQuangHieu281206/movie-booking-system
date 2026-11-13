import React, { useState } from "react";
import { byId } from "../../lib/admin.js";
import { Button, ErrorBox, Icon, Modal } from "../../components/UI.jsx";

function fieldOptions(field, ctx) {
  if (field.options) return field.options;
  return (ctx[field.optionsKey] || []).map((item) => ({
    value: item[field.optionValue],
    label:
      field.optionsKey === "rooms"
        ? `${item.name} — ${byId(ctx.cinemas, "cinemaId", "name", item.cinemaId)}`
        : item[field.optionLabel],
  }));
}

export default function ResourceForm({ api, resource, row, ctx, state, onClose, onSaved }) {
  const editing = Boolean(row);
  const [values, setValues] = useState(() =>
    resource.toForm(row || null, ctx, state),
  );
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update(name, value) {
    setValues((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const payload = resource.toPayload(values, editing);
      if (editing)
        await api.admin.update(resource.updatePath, row[resource.idKey], payload);
      else await api.admin.create(resource.createPath, payload);
      onSaved(editing ? "Đã cập nhật thành công." : "Đã thêm mới thành công.");
    } catch (cause) {
      setError(cause.message || "Không lưu được dữ liệu.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      title={`${editing ? "Sửa" : "Thêm"} ${resource.label.toLowerCase()}`}
      onClose={onClose}
      busy={busy}
      wide
      className="admin-dialog"
    >
      <form className="admin-form" onSubmit={submit}>
        {error && <ErrorBox message={error} />}
        <div className="admin-form-grid">
          {resource.fields.map((field) => {
            const wide = field.wide || field.type === "textarea" || field.type === "genres";
            const readOnly = editing && field.readOnlyOnEdit;
            const required = field.required && !(editing && field.optionalOnEdit);
            const label = (
              <>
                {field.label}
                {required && <span aria-hidden="true"> *</span>}
              </>
            );
            if (field.type === "genres")
              return (
                <fieldset className="admin-field admin-field-wide" key={field.name}>
                  <legend>{label}</legend>
                  <div className="admin-check-list">
                    {fieldOptions(field, ctx).map((option) => {
                      const checked = (values[field.name] || [])
                        .map(String)
                        .includes(String(option.value));
                      return (
                        <label className="admin-check" key={option.value}>
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() =>
                              update(
                                field.name,
                                checked
                                  ? values[field.name].filter(
                                      (value) => String(value) !== String(option.value),
                                    )
                                  : [...(values[field.name] || []), option.value],
                              )
                            }
                          />
                          <span>{option.label}</span>
                        </label>
                      );
                    })}
                  </div>
                </fieldset>
              );
            if (field.type === "select")
              return (
                <label className={`admin-field ${wide ? "admin-field-wide" : ""}`} key={field.name}>
                  {label}
                  <select
                    value={values[field.name] ?? ""}
                    required={required}
                    onChange={(event) => update(field.name, event.target.value)}
                  >
                    <option value="">— Chọn —</option>
                    {fieldOptions(field, ctx).map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  {field.help && <small>{field.help}</small>}
                </label>
              );
            if (field.type === "textarea")
              return (
                <label className="admin-field admin-field-wide" key={field.name}>
                  {label}
                  <textarea
                    value={values[field.name] ?? ""}
                    required={required}
                    maxLength={field.maxLength}
                    rows={4}
                    onChange={(event) => update(field.name, event.target.value)}
                  />
                  {field.help && <small>{field.help}</small>}
                </label>
              );
            return (
              <label className={`admin-field ${wide ? "admin-field-wide" : ""}`} key={field.name}>
                {label}
                <input
                  type={field.type || "text"}
                  value={values[field.name] ?? ""}
                  required={required}
                  disabled={readOnly}
                  readOnly={readOnly}
                  min={field.min}
                  max={field.max}
                  minLength={field.minLength}
                  maxLength={field.maxLength}
                  step={field.step}
                  pattern={field.pattern}
                  title={field.title}
                  inputMode={field.inputMode}
                  placeholder={
                    editing && field.optionalOnEdit ? "Để trống nếu không đổi" : undefined
                  }
                  onChange={(event) => update(field.name, event.target.value)}
                />
                {field.help && <small>{field.help}</small>}
              </label>
            );
          })}
        </div>
        <div className="admin-form-actions">
          <Button type="button" onClick={onClose} disabled={busy}>
            Huỷ
          </Button>
          <Button primary type="submit" disabled={busy}>
            {busy ? "Đang lưu…" : editing ? "Lưu thay đổi" : "Thêm mới"}
            <Icon name="check" size={16} />
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// Ngày lưu dạng YYYY-MM-DD, hiển thị và nhập theo dd/mm/yyyy.
