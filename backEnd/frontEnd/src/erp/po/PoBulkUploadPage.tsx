import { useRef, useState, type CSSProperties } from "react";
import { useNavigate } from "react-router-dom";
import * as XLSX from "xlsx";
import { BulkPoPreviewResponse, previewPoBulk, uploadPoBulk } from "../api";

// 백엔드 파서가 인식하는 헤더명(한글). 템플릿과 순서를 맞춘다.
// 발주 라인은 '공급사코드 + 품목코드'로 지정하고, 같은 발주로 묶으려면 '발주그룹'을 같은 값으로 둔다.
const TEMPLATE_HEADERS = ["공급사코드", "품목코드", "수량", "단가", "납기일", "비고", "발주그룹"];

/** 빈 템플릿(.xlsx) 다운로드 */
function downloadTemplate() {
    const aoa: (string | number)[][] = [
        TEMPLATE_HEADERS,
        // 예시: 같은 발주그룹(A)으로 묶인 2개 라인 + 다른 발주(B) 1개
        ["V001", "ITEM-001", 10, "", "2026-07-10", "7월 정기발주", "A"],
        ["V001", "ITEM-002", 5, 2000, "2026-07-10", "", "A"],
        ["V002", "ITEM-003", 3, "", "2026-07-15", "", "B"],
    ];
    const ws = XLSX.utils.aoa_to_sheet(aoa);
    ws["!cols"] = [{ wch: 12 }, { wch: 14 }, { wch: 8 }, { wch: 10 }, { wch: 14 }, { wch: 16 }, { wch: 10 }];
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "대량발주");
    XLSX.writeFile(wb, "대량발주_양식.xlsx");
}

export default function PoBulkUploadPage() {
    const navigate = useNavigate();
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<BulkPoPreviewResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    const onSelectFile = (f: File | null) => {
        setFile(f);
        setPreview(null);
        setErrorMsg(null);
    };

    const readErr = (e: any, fallback: string) =>
        e?.response?.headers?.["x-error-detail"] ??
        e?.response?.data?.message ??
        e?.message ??
        fallback;

    const handlePreview = async () => {
        if (!file) {
            alert("엑셀 파일을 먼저 선택해 주세요.");
            return;
        }
        setLoading(true);
        setErrorMsg(null);
        setPreview(null);
        try {
            setPreview(await previewPoBulk(file));
        } catch (e: any) {
            setErrorMsg(readErr(e, "미리보기 중 오류가 발생했습니다."));
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = async () => {
        if (!file || !preview?.confirmable) return;
        if (!window.confirm(`정상 ${preview.validRows}건(발주 ${preview.poCount}건)을 등록합니다. 진행할까요?`)) return;

        setSaving(true);
        setErrorMsg(null);
        try {
            const res = await uploadPoBulk(file);
            if (res.failRows > 0) {
                const detail = res.errors.map((x) => `${x.row}행: ${x.message}`).join("\n");
                setErrorMsg("확정 중 검증 오류가 발생하여 저장되지 않았습니다.\n" + detail);
                await handlePreview();
                return;
            }
            alert(`발주 등록 완료: ${res.createdPos}건 생성 (라인 ${res.successRows}건)`);
            navigate("/erp/po");
        } catch (e: any) {
            setErrorMsg(readErr(e, "저장 중 오류가 발생했습니다."));
        } finally {
            setSaving(false);
        }
    };

    const cell: CSSProperties = { border: "1px solid #ccc", padding: "4px 6px", fontSize: 13 };
    const th: CSSProperties = { ...cell, background: "#f2f2f2", whiteSpace: "nowrap" };

    return (
        <div>
            <h2>대량 발주 엑셀 업로드</h2>

            <div style={{ marginBottom: 10, display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                <button type="button" onClick={downloadTemplate}>
                    양식 다운로드
                </button>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept=".xlsx,.xls"
                    onChange={(e) => onSelectFile(e.target.files?.[0] ?? null)}
                />
                <button type="button" onClick={handlePreview} disabled={!file || loading}>
                    {loading ? "미리보기 중..." : "미리보기"}
                </button>
                <button type="button" onClick={() => navigate("/erp/po")}>
                    목록으로
                </button>
            </div>

            <div style={{ marginBottom: 10, color: "#666", fontSize: 13 }}>
                ※ 필수 컬럼: <b>공급사코드</b>, <b>품목코드</b>, <b>수량</b>, <b>납기일</b> / 선택: 단가(미입력 시 품목 표준가), 비고, 발주그룹
                <br />
                ※ 여러 라인을 한 발주로 묶으려면 <b>발주그룹</b>에 같은 값을 넣으세요. 비우면 (공급사코드 + 납기일)이 같은 라인끼리 한 발주로 묶입니다.
                <br />
                ※ 생성되는 발주는 <b>DRAFT(임시저장)</b> 상태입니다.
            </div>

            {errorMsg && (
                <div style={{ marginBottom: 10, padding: 8, background: "#fdecea", border: "1px solid #e74c3c", color: "#a93226", whiteSpace: "pre-wrap" }}>
                    {errorMsg}
                </div>
            )}

            {preview && (
                <>
                    <div style={{ marginBottom: 8, display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap" }}>
                        <span>전체 <b>{preview.totalRows}</b>건</span>
                        <span style={{ color: "#2e7d32" }}>정상 <b>{preview.validRows}</b>건</span>
                        <span style={{ color: "#c0392b" }}>오류 <b>{preview.errorRows}</b>건</span>
                        <span>→ 발주 <b>{preview.poCount}</b>건 생성 예정</span>
                        <button
                            type="button"
                            onClick={handleConfirm}
                            disabled={!preview.confirmable || saving}
                            style={{ marginLeft: "auto", padding: "6px 14px" }}
                            title={preview.confirmable ? "" : "오류 행이 있어 확정할 수 없습니다. 엑셀을 수정 후 다시 미리보기 하세요."}
                        >
                            {saving ? "저장 중..." : "확정(저장)"}
                        </button>
                    </div>

                    {!preview.confirmable && preview.errorRows > 0 && (
                        <div style={{ marginBottom: 8, color: "#c0392b", fontSize: 13 }}>
                            ※ 오류 행이 있어 저장할 수 없습니다. 빨간색으로 표시된 행을 수정한 뒤 다시 업로드해 주세요.
                        </div>
                    )}

                    <table style={{ borderCollapse: "collapse", width: "100%" }}>
                        <thead>
                            <tr>
                                <th style={th}>행</th>
                                <th style={th}>발주묶음</th>
                                <th style={th}>공급사코드</th>
                                <th style={th}>공급사명</th>
                                <th style={th}>품목코드</th>
                                <th style={th}>품목명</th>
                                <th style={th}>수량</th>
                                <th style={th}>단가</th>
                                <th style={th}>금액</th>
                                <th style={th}>납기일</th>
                                <th style={th}>비고</th>
                                <th style={th}>결과</th>
                            </tr>
                        </thead>
                        <tbody>
                            {preview.rows.map((r) => (
                                <tr key={r.rowNo} style={{ background: r.valid ? undefined : "#fdecea" }}>
                                    <td style={{ ...cell, textAlign: "center" }}>{r.rowNo}</td>
                                    <td style={cell}>{r.groupLabel ?? ""}</td>
                                    <td style={cell}>{r.vendorCode ?? ""}</td>
                                    <td style={cell}>{r.vendorName ?? ""}</td>
                                    <td style={cell}>{r.itemCode ?? ""}</td>
                                    <td style={cell}>{r.itemName ?? ""}</td>
                                    <td style={{ ...cell, textAlign: "right" }}>{r.quantity ?? ""}</td>
                                    <td style={{ ...cell, textAlign: "right" }}>{r.unitPrice ?? ""}</td>
                                    <td style={{ ...cell, textAlign: "right" }}>{r.amount ?? ""}</td>
                                    <td style={cell}>{r.deliveryDate ?? ""}</td>
                                    <td style={cell}>{r.etc ?? ""}</td>
                                    <td style={{ ...cell, color: r.valid ? "#2e7d32" : "#c0392b" }}>
                                        {r.valid ? "정상" : r.error}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </>
            )}
        </div>
    );
}
