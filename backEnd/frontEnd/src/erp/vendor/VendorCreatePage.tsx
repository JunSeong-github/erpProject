import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { checkVendorDuplicate, checkVendorNameDuplicate, createVendor, deleteVendor, getVendorDetail, getVendorInUse, updateVendor } from "../api";
import { useQuery } from "@tanstack/react-query";

export default function VendorCreatePage() {

    const { code } = useParams();
    const isEdit = Boolean(code);

    const [vendorCode, setVendorCode] = useState("");
    const [vendorName, setVendorName] = useState("");
    const [codeOk, setCodeOk] = useState(false);       // 공급사코드 중복확인 통과 여부(수정 시 코드 고정이라 true)
    const [nameOk, setNameOk] = useState(false);       // 공급사명 중복확인 통과 여부
    const [isChecking, setIsChecking] = useState(false);
    const [isCheckingName, setIsCheckingName] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const navigate = useNavigate();
    const location = useLocation();
    const statePage = location.state?.page ?? 0;
    const stateSearchCondition = location.state?.searchCondition ?? {};

    const goBackToList = () => {
        const params = new URLSearchParams({
            page: String(statePage)
        });

        if (stateSearchCondition.vendorName) {
            params.append("vendorName", stateSearchCondition.vendorName);
        }
        if (stateSearchCondition.vendorCode) {
            params.append("vendorCode", stateSearchCondition.vendorCode);
        }

        navigate(`/erp/vendor?${params.toString()}`);
    };

    const { data: vendorDetail } = useQuery({
        queryKey: ["vendorDetail", code],
        queryFn: () => getVendorDetail(String(code)),
        enabled: isEdit,
    });

    // 발주에 사용된 공급사인지 여부. 사용됐으면 수정·삭제를 막는다.
    const { data: inUse } = useQuery({
        queryKey: ["vendorInUse", code],
        queryFn: () => getVendorInUse(String(code)),
        enabled: isEdit,
    });
    const locked = isEdit && inUse === true;

    useEffect(() => {
        if (!vendorDetail) return;

        setVendorCode(vendorDetail.vendorCode);
        setVendorName(vendorDetail.vendorName);
        // 수정 진입 시 코드는 고정, 기존 이름도 유효하므로 통과 상태로 시작
        setCodeOk(true);
        setNameOk(true);
    }, [vendorDetail]);

    const checkDuplicate = async () => {
        if (!vendorCode.trim()) {
            alert("공급사코드를 입력해주세요.");
            return;
        }

        setIsChecking(true);

        try {
            const exists = await checkVendorDuplicate(vendorCode);
            setCodeOk(!exists);
            alert(exists ? "이미 존재하는 공급사코드입니다." : "사용 가능한 공급사코드입니다.");
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsChecking(false);
        }
    };

    const checkNameDuplicate = async () => {
        if (!vendorName.trim()) {
            alert("공급사명을 입력해주세요.");
            return;
        }

        if (vendorDetail && vendorDetail.vendorName === vendorName) {
            setNameOk(true);
            alert("사용 가능한 공급사명입니다.");
            return;
        }

        setIsCheckingName(true);
        try {
            const exists = await checkVendorNameDuplicate(vendorName);
            setNameOk(!exists);
            alert(exists ? "이미 존재하는 공급사명입니다." : "사용 가능한 공급사명입니다.");
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsCheckingName(false);
        }
    };

    const handleSave = async () => {
        if (locked) {
            alert("발주에 사용된 공급사는 수정할 수 없습니다.");
            return;
        }
        if (!vendorCode) {
            alert("공급사코드를 입력하세요.");
            return;
        }

        if (!vendorName) {
            alert("공급사명을 입력하세요.");
            return;
        }

        if (!codeOk) {
            alert("공급사코드 중복확인을 해주세요.");
            return;
        }

        if (!nameOk) {
            alert("공급사명 중복확인을 해주세요.");
            return;
        }

        const payload = {
            vendorCode: vendorCode,
            vendorName: vendorName,
        };

        try {
            setIsSaving(true);

            if (isEdit) {
                await updateVendor(String(code), payload); // 수정
            } else {
                await createVendor(payload);                // 신규등록
            }

            alert(isEdit ? "수정되었습니다." : "저장되었습니다.");
            if (!isEdit) goBackToList();
        } catch (e) {
            console.error(e);
            alert("오류 발생");
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!code) return;
        if (locked) {
            alert("발주에 사용된 공급사는 삭제할 수 없습니다.");
            return;
        }

        const ok = confirm("정말로 이 공급사를 삭제할까요?");
        if (!ok) return;

        try {
            await deleteVendor(String(code));
            alert("삭제되었습니다.");
            goBackToList();
        } catch (e: any) {
            console.error(e);
            alert(e?.response?.data?.message ?? e?.message ?? "오류 발생");
        }
    };

    return (
        <div>
            <h2>{isEdit ? "공급사 수정" : "공급사 작성"}</h2>

            {locked && (
                <div
                    style={{
                        color: "#b00020",
                        background: "#fff3f3",
                        border: "1px solid #f0c0c0",
                        borderRadius: "4px",
                        padding: "8px 10px",
                        marginBottom: "10px",
                    }}
                >
                    ⚠ 이 공급사는 발주에 사용된 이력이 있어 <b>수정·삭제할 수 없습니다.</b>
                </div>
            )}

            <div>
                <label>공급사 코드 :&nbsp;</label>
                <input
                    type="text"
                    value={vendorCode}
                    readOnly={isEdit}
                    style={{ width: "120px" }}
                    onChange={(e) => {
                        setVendorCode(e.target.value);
                        setCodeOk(false);
                    }}
                />

                {!isEdit && (
                    <button
                        type="button"
                        onClick={checkDuplicate}
                        disabled={isChecking}
                    >
                        {isChecking ? "확인중..." : "중복확인"}
                    </button>
                )}

                <label>
                    &nbsp;공급사 명 :&nbsp;
                    <input
                        type="text"
                        value={vendorName}
                        style={{ width: "150px" }}
                        disabled={locked}
                        onChange={(e) => {
                            setVendorName(e.target.value);
                            setNameOk(false);
                        }}
                    />
                </label>

                <button
                    type="button"
                    onClick={checkNameDuplicate}
                    disabled={isCheckingName || locked}
                >
                    {isCheckingName ? "확인중..." : "중복확인"}
                </button>
            </div>

            <div style={{ marginTop: "12px" }}>
                <button type="button" onClick={handleSave} disabled={isSaving || locked}>
                    {isSaving ? "저장 중..." : isEdit ? "수정" : "저장"}
                </button>

                {isEdit && (
                    <button type="button" onClick={handleDelete} disabled={locked} style={{ marginLeft: "8px" }}>
                        삭제
                    </button>
                )}

                <button
                    type="button"
                    onClick={goBackToList}
                    style={{ marginLeft: "8px" }}
                >
                    목록
                </button>
            </div>
        </div>
    );
}
