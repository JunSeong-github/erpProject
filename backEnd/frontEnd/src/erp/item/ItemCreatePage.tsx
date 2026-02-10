import {useParams} from "react-router-dom";
import {useEffect, useState} from "react";
import {checkItemDuplicate, createItem, deleteItem, getItemDetail, ItemCreateRequest, updateItem} from "../api";
import {useQuery} from "@tanstack/react-query";


export default function ItemCreatePage(){

    const { id } = useParams();
    const isEdit = Boolean(id);

    const [itemCode, setItemCode] = useState("");
    const [itemName, setItemName] = useState("");
    const [standardPrice, setStandardPrice] = useState("");
    const [isDuplicate, setIsDuplicate] = useState(false);
    const [isChecking, setIsChecking] = useState(false);
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

    }, [itemDetail]);

    const checkDuplicate = async ()=>{

        if(!itemCode.trim()){
            alert("품목코드를 입력해주세요.");
            return;
        }

        setIsChecking(true);

        try {

            let exists = false;

            if(!(itemDetail && (itemDetail.itemCode==itemCode))){
                exists = await checkItemDuplicate(itemCode);
            }

            setIsDuplicate(exists);

            if (exists) {
                alert("이미 존재하는 품목코드입니다.");
            } else {
                alert("사용 가능한 품목코드입니다.");
            }
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsChecking(false);
        }

    }

    const handleSave= async() =>{
        debugger;
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

        const payload ={
            itemCode:itemCode,
            itemName:itemName,
            standardPrice:standardPrice
        };

        try{
            setIsSaving(true);

            const url=isEdit
            ? await updateItem(Number(id),payload) // 수정
            : await createItem(payload); // 신규등록

            alert(isEdit? "수정되었습니다." : "저장되었습니다.");
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
        } catch(e){
            console.error(e);
            alert("오류 발생");
        }
       
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
                            setIsDuplicate(false);
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
                    품목 이름 :&nbsp;
                    <input
                        type="text"
                        value={itemName}
                        style={{width: "80px"}}
                        onChange={(e) => setItemName(e.target.value)}
                    />
                </label>

                <label>
                    품목 가격 :&nbsp;
                    <input
                        type="text"
                        value={standardPrice}
                        style={{width: "80px"}}
                        onChange={(e) => setStandardPrice(e.target.value)}
                    />
                </label>

            </div>
            
            <button type="button" onClick={handleSave}>
                {isSaving ? "저장 중..." : isEdit ? "수정" : "저장"}
            </button>

            <button type="button" onClick={handleDelete}>
              삭제
            </button>

        </div>
    );

}