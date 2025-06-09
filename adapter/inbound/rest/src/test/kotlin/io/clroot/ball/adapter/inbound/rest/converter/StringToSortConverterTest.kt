package io.clroot.ball.adapter.inbound.rest.converter

import io.clroot.ball.domain.model.paging.Direction
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StringToSortConverterTest : DescribeSpec({

    val converter = StringToSortConverter()

    describe("StringToSortConverter") {
        
        describe("단일 정렬 필드") {
            
            it("ASC 정렬을 올바르게 변환해야 한다") {
                // given
                val source = "name:asc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isSorted shouldBe true
                result.orders shouldHaveSize 1
                result.orders[0].property shouldBe "name"
                result.orders[0].direction shouldBe Direction.ASC
            }
            
            it("DESC 정렬을 올바르게 변환해야 한다") {
                // given
                val source = "createdAt:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isSorted shouldBe true
                result.orders shouldHaveSize 1
                result.orders[0].property shouldBe "createdAt"
                result.orders[0].direction shouldBe Direction.DESC
            }
            
            it("방향 지정 없는 필드는 ASC로 처리해야 한다") {
                // given
                val source = "email"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isSorted shouldBe true
                result.orders shouldHaveSize 1
                result.orders[0].property shouldBe "email"
                result.orders[0].direction shouldBe Direction.ASC
            }
        }
        
        describe("복수 정렬 필드") {
            
            it("여러 정렬 필드를 올바르게 처리해야 한다") {
                // given
                val source = "name:asc,age:desc,email"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isSorted shouldBe true
                result.orders shouldHaveSize 3
                
                result.orders[0].property shouldBe "name"
                result.orders[0].direction shouldBe Direction.ASC
                
                result.orders[1].property shouldBe "age"
                result.orders[1].direction shouldBe Direction.DESC
                
                result.orders[2].property shouldBe "email"
                result.orders[2].direction shouldBe Direction.ASC
            }
            
            it("복잡한 다중 정렬을 처리해야 한다") {
                // given
                val source = "lastName:desc,firstName:asc,email:asc,createdAt:desc,id:asc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 5
                
                result.orders[0].property shouldBe "lastName"
                result.orders[0].direction shouldBe Direction.DESC
                
                result.orders[1].property shouldBe "firstName"
                result.orders[1].direction shouldBe Direction.ASC
                
                result.orders[2].property shouldBe "email"
                result.orders[2].direction shouldBe Direction.ASC
                
                result.orders[3].property shouldBe "createdAt"
                result.orders[3].direction shouldBe Direction.DESC
                
                result.orders[4].property shouldBe "id"
                result.orders[4].direction shouldBe Direction.ASC
            }
        }
        
        describe("공백 처리") {
            
            it("앞뒤 공백을 제거해야 한다") {
                // given
                val source = " name:asc , age:desc , email "
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 3
                result.orders[0].property shouldBe "name"
                result.orders[1].property shouldBe "age"
                result.orders[2].property shouldBe "email"
            }
            
            it("중간의 빈 항목을 무시해야 한다") {
                // given
                val source = "name:asc,,age:desc,"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "name"
                result.orders[1].property shouldBe "age"
            }
            
            it("공백만 있는 항목을 무시해야 한다") {
                // given
                val source = "name:asc, , age:desc,   "
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "name"
                result.orders[1].property shouldBe "age"
            }
        }
        
        describe("엣지 케이스") {
            
            it("빈 문자열은 unsorted를 반환해야 한다") {
                // given
                val source = ""
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isUnsorted shouldBe true
                result.orders shouldHaveSize 0
            }
            
            it("공백만 있는 문자열은 unsorted를 반환해야 한다") {
                // given
                val source = "   "
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isUnsorted shouldBe true
                result.orders shouldHaveSize 0
            }
            
            it("콤마만 있는 문자열은 unsorted를 반환해야 한다") {
                // given
                val source = ",,,"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isUnsorted shouldBe true
                result.orders shouldHaveSize 0
            }
        }
        
        describe("특수 문자가 포함된 필드명") {
            
            it("언더스코어가 포함된 필드명을 처리해야 한다") {
                // given
                val source = "first_name:asc,last_name:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "first_name"
                result.orders[1].property shouldBe "last_name"
            }
            
            it("점이 포함된 중첩 필드명을 처리해야 한다") {
                // given
                val source = "user.name:asc,user.profile.email:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "user.name"
                result.orders[1].property shouldBe "user.profile.email"
            }
            
            it("하이픈이 포함된 필드명을 처리해야 한다") {
                // given
                val source = "created-at:desc,updated-at:asc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "created-at"
                result.orders[1].property shouldBe "updated-at"
            }
            
            it("숫자가 포함된 필드명을 처리해야 한다") {
                // given
                val source = "field1:asc,field2:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "field1"
                result.orders[1].property shouldBe "field2"
            }
        }
        
        describe("잘못된 형식 처리") {
            
            it("여러 콜론이 있는 필드명은 마지막 :desc/:asc만 방향으로 인식해야 한다") {
                // given
                val source = "namespace:field:name:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 1
                result.orders[0].property shouldBe "namespace:field:name"
                result.orders[0].direction shouldBe Direction.DESC
            }
            
            it("잘못된 방향 지시자는 전체를 필드명으로 처리해야 한다") {
                // given
                val source = "name:invalid"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 1
                result.orders[0].property shouldBe "name:invalid"
                result.orders[0].direction shouldBe Direction.ASC
            }
            
            it("콜론으로 끝나는 필드는 콜론까지 필드명으로 처리해야 한다") {
                // given
                val source = "name:"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 1
                result.orders[0].property shouldBe "name:"
                result.orders[0].direction shouldBe Direction.ASC
            }
        }
        
        describe("대소문자 처리") {
            
            it("방향 지시자는 소문자만 인식해야 한다") {
                // given
                val source = "name:ASC,age:DESC"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.orders shouldHaveSize 2
                result.orders[0].property shouldBe "name:ASC"
                result.orders[0].direction shouldBe Direction.ASC
                result.orders[1].property shouldBe "age:DESC"
                result.orders[1].direction shouldBe Direction.ASC
            }
        }
        
        describe("Sort 객체 동작") {
            
            it("정렬된 Sort 객체의 isSorted가 true여야 한다") {
                // given
                val source = "name:asc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isSorted shouldBe true
                result.isUnsorted shouldBe false
            }
            
            it("정렬되지 않은 Sort 객체의 isUnsorted가 true여야 한다") {
                // given
                val source = ""
                
                // when
                val result = converter.convert(source)
                
                // then
                result.isUnsorted shouldBe true
                result.isSorted shouldBe false
            }
        }
    }
})
