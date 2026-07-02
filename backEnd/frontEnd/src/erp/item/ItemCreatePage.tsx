import {useLocation, useNavigate, useParams} from "react-router-dom";
import {useEffect, useState} from "react";
import {checkItemDuplicate, checkItemNameDuplicate, createItem, deleteItem, getItemDetail, updateItem} from "../api";
import {useQuery} from "@tanstack/react-query";


export default function ItemCreatePage(){

    const { id } = useParams();
    const isEdit = Boolean(id);

    const [itemCode, setItemCode] = useState("");
    const [itemName, setItemName] = useState("");
    const [standardPrice, setStandardPrice] = useState("");
    const [codeOk, setCodeOk] = useState(false);       // 품목코드 중복확인 통과 여부
    const [nameOk, setNameOk] = useState(false);       // 품목이름 중복확인 통과 여부
    const [isChecking, setIsChecking] = useState(false);
    const [isCheckingName, setIsCheckingName] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const { data:itemDetail } =useQuery({
        queryKey:["itemDetail", id],
        queryFn:()=> getItemDetail(Number(id)),
        enabled:isEdit,
    });

    useEffect(() => {
        if(!itemDetail) return;

        setItemCode(itemDetail.itemCode);
        setItemName(itemDetail.itemName);
        setStandardPrice(String(itemDetail.standardPrice));
        // 수정 진입 시 기존 값은 이미 유효하므로 통과 상태로 시작
        setCodeOk(true);
        setNameOk(true);

    }, [itemDetail]);

    const checkDuplicate = async ()=>{

        if(!itemCode.trim()){
            alert("품목코드를 입력해주세요.");
            return;
        }

        // 수정 중 기존 코드 그대로면 통과 처리
        if(itemDetail && itemDetail.itemCode === itemCode){
            setCodeOk(true);
            alert("사용 가능한 품목코드입니다.");
            return;
        }

        setIsChecking(true);
        try {
            const exists = await checkItemDuplicate(itemCode);
            setCodeOk(!exists);
            alert(exists ? "이미 존재하는 품목코드입니다." : "사용 가능한 품목코드입니다.");
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsChecking(false);
        }
    }

    const checkNameDuplicate = async ()=>{

        if(!itemName.trim()){
            alert("품목이름을 입력해주세요.");
            return;
        }

        if(itemDetail && itemDetail.itemName === itemName){
            setNameOk(true);
            alert("사용 가능한 품목이름입니다.");
            return;
        }

        setIsCheckingName(true);
        try {
            const exists = await checkItemNameDuplicate(itemName);
            setNameOk(!exists);
            alert(exists ? "이미 존재하는 품목이름입니다." : "사용 가능한 품목이름입니다.");
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsCheckingName(false);
        }
    }

    const handleSave= async() =>{
        if(!itemCode){
            alert("품목코드를 입력하세요.");
            return;
        }

        if(!itemName){
            alert("품목이름을 입력하세요.");
            return;
        }

        if(!standardPrice){
            alert("품목가격을 입력하세요.");
            return;
        }

        if(!codeOk){
            alert("품목코드 중복확인을 해주세요.");
            return;
        }

        if(!nameOk){
            alert("품목이름 중복확인을 해주세요.");
            return;
        }

        const payload ={
            itemCode:itemCode,
            itemName:itemName,
            standardPrice:standardPrice
        };

        try{
            setIsSaving(true);

            if(isEdit){
                await updateItem(Number(id),payload); // 수정
            } else {
                await createItem(payload); // 신규등록
            }

            alert(isEdit? "수정되었습니다." : "저장되었습니다.");
            if(!isEdit) goBackToList();
        } catch (e){
            console.error(e);
            alert("오류 발생");
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!id) return;

        const ok = confirm("정말로 이 물품을 삭제할까요?");
        if (!ok) return;

        try{
            await deleteItem(Number(id));

            alert("삭제되었습니다.")
            goBackToList();
        } catch(e: any){
            console.error(e);
            alert(e?.response?.data?.message ?? e?.message ?? "오류 발생");
        }

    };

    const navigate = useNavigate();
    const location = useLocation();
    const statePage = location.state?.page ?? 0;
    const stateSearchCondition = location.state?.searchCondition ?? {};

    const goBackToList = () => {

        const params = new URLSearchParams({
            page: String(statePage)
        });

        if (stateSearchCondition.itemName) {
            params.append("itemName", stateSearchCondition.itemName);
        }

        navigate(`/erp/item?${params.toString()}`);
    };


    return(
        <div>
            <h2>{isEdit ? "품목 수정" : "품목 작성"}</h2>

            <div>

                <label>품목 코드 :&nbsp;</label>
                    <input
                        type="text"
                        value={itemCode}
                        style={{width: "80px"}}
                        onChange={(e) =>{
                            setItemCode(e.target.value);
                            setCodeOk(false);
                        }}
                    />

                <button
                    type="button"
                    onClick={checkDuplicate}
                    disabled={isChecking}
                >
                    {isChecking ? "확인중..." : "중복확인"}
                </button>

                <label>
                    &nbsp;품목 이름 :&nbsp;
                    <input
                        type="text"
                        value={itemName}
                        style={{width: "80px"}}
                        onChange={(e) => {
                            setItemName(e.target.value);
                            setNameOk(false);
                        }}
                    />
                </label>

                <button
                    type="button"
                    onClick={checkNameDuplicate}
                    disabled={isCheckingName}
                >
                    {isCheckingName ? "확인중..." : "중복확인"}
                </button>

                <label>
                    &nbsp;품목 가격 :&nbsp;
                    <input
                        type="text"
                        value={standardPrice}
                        style={{width: "80px"}}
                        onChange={(e) => setStandardPrice(e.target.value)}
                    />
                </label>

            </div>

            <button type="button" onClick={handleSave} disabled={isSaving}>
                {isSaving ? "저장 중..." : isEdit ? "수정" : "저장"}
            </button>

            {isEdit && (
                <button type="button" onClick={handleDelete}>
                  삭제
                </button>
            )}

            <button
                type="button"
                onClick={goBackToList}
                style={{ marginBottom: "10px" }}
            >
                목록
            </button>

        </div>
    );

}
