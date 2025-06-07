package io.clroot.ball.adapter.inbound.rest.converter

import io.clroot.ball.domain.model.core.paging.Direction
import io.clroot.ball.domain.model.core.paging.PageRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StringToPageRequestConverterTest : DescribeSpec({

    val converter = StringToPageRequestConverter()

    describe("StringToPageRequestConverter") {
        
        describe("기본 페이징 변환") {
            
            it("페이지와 사이즈만 있는 경우") {
                // given
                val source = "0,20"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 0
                result.size shouldBe 20
                result.sort.isUnsorted shouldBe true
            }
            
            it("페이지만 있는 경우 기본 사이즈를 사용해야 한다") {
                // given
                val source = "2"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 2
                result.size shouldBe PageRequest.DEFAULT_SIZE
                result.sort.isUnsorted shouldBe true
            }
            
            it("빈 문자열인 경우 기본값을 사용해야 한다") {
                // given
                val source = ""
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 0
                result.size shouldBe PageRequest.DEFAULT_SIZE
                result.sort.isUnsorted shouldBe true
            }
            
            it("잘못된 페이지 번호는 0으로 처리해야 한다") {
                // given
                val source = "invalid,20"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 0
                result.size shouldBe 20
            }
            
            it("잘못된 사이즈는 기본값으로 처리해야 한다") {
                // given
                val source = "1,invalid"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 1
                result.size shouldBe 30
            }
        }
        
        describe("정렬 포함 변환") {
            
            it("단일 ASC 정렬을 처리해야 한다") {
                // given
                val source = "0,10,name:asc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 0
                result.size shouldBe 10
                result.sort.isSorted shouldBe true
                result.sort.orders shouldHaveSize 1
                result.sort.orders[0].property shouldBe "name"
                result.sort.orders[0].direction shouldBe Direction.ASC
            }
            
            it("단일 DESC 정렬을 처리해야 한다") {
                // given
                val source = "1,5,createdAt:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 1
                result.size shouldBe 5
                result.sort.isSorted shouldBe true
                result.sort.orders shouldHaveSize 1
                result.sort.orders[0].property shouldBe "createdAt"
                result.sort.orders[0].direction shouldBe Direction.DESC
            }
            
            it("방향 지정 없는 정렬은 ASC로 처리해야 한다") {
                // given
                val source = "0,10,name"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 1
                result.sort.orders[0].property shouldBe "name"
                result.sort.orders[0].direction shouldBe Direction.ASC
            }
            
            it("복수 정렬 필드를 처리해야 한다") {
                // given
                val source = "0,10,name:asc,age:desc,email"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 3
                
                result.sort.orders[0].property shouldBe "name"
                result.sort.orders[0].direction shouldBe Direction.ASC
                
                result.sort.orders[1].property shouldBe "age"
                result.sort.orders[1].direction shouldBe Direction.DESC
                
                result.sort.orders[2].property shouldBe "email"
                result.sort.orders[2].direction shouldBe Direction.ASC
            }
        }
        
        describe("공백 처리") {
            
            it("공백이 포함된 문자열을 올바르게 처리해야 한다") {
                // given
                val source = " 1 , 15 , name:asc , email:desc "
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 1
                result.size shouldBe 15
                result.sort.orders shouldHaveSize 2
                result.sort.orders[0].property shouldBe "name"
                result.sort.orders[1].property shouldBe "email"
            }
        }
        
        describe("엣지 케이스") {
            
            it("빈 정렬 필드는 무시해야 한다") {
                // given
                val source = "0,10,name:asc,,email:desc,"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 2
                result.sort.orders[0].property shouldBe "name"
                result.sort.orders[1].property shouldBe "email"
            }
            
            it("콜론만 있는 잘못된 정렬 형식은 무시해야 한다") {
                // given
                val source = "0,10,:asc,name:,valid:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 1
                result.sort.orders[0].property shouldBe "valid"
                result.sort.orders[0].direction shouldBe Direction.DESC
            }
            
            it("잘못된 정렬 방향은 ASC로 처리해야 한다") {
                // given
                val source = "0,10,name:invalid"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 1
                result.sort.orders[0].property shouldBe "name:invalid"
                result.sort.orders[0].direction shouldBe Direction.ASC
            }
            
            it("정렬 파라미터가 많은 콜론을 포함해도 처리해야 한다") {
                // given
                val source = "0,10,complex:field:name:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 1
                result.sort.orders[0].property shouldBe "complex:field:name"
                result.sort.orders[0].direction shouldBe Direction.DESC
            }
        }
        
        describe("복합 시나리오") {
            
            it("복잡한 페이징과 정렬 조합을 처리해야 한다") {
                // given
                val source = "2,25,lastName:desc,firstName:asc,email,createdAt:desc,id:asc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.page shouldBe 2
                result.size shouldBe 25
                result.sort.orders shouldHaveSize 5
                
                // 정렬 순서 확인
                result.sort.orders[0].property shouldBe "lastName"
                result.sort.orders[0].direction shouldBe Direction.DESC
                
                result.sort.orders[1].property shouldBe "firstName"
                result.sort.orders[1].direction shouldBe Direction.ASC
                
                result.sort.orders[2].property shouldBe "email"
                result.sort.orders[2].direction shouldBe Direction.ASC
                
                result.sort.orders[3].property shouldBe "createdAt"
                result.sort.orders[3].direction shouldBe Direction.DESC
                
                result.sort.orders[4].property shouldBe "id"
                result.sort.orders[4].direction shouldBe Direction.ASC
            }
        }
        
        describe("특수 문자 처리") {
            
            it("언더스코어가 포함된 필드명을 처리해야 한다") {
                // given
                val source = "0,10,first_name:asc,last_name:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 2
                result.sort.orders[0].property shouldBe "first_name"
                result.sort.orders[1].property shouldBe "last_name"
            }
            
            it("점이 포함된 중첩 필드명을 처리해야 한다") {
                // given
                val source = "0,10,user.name:asc,user.profile.email:desc"
                
                // when
                val result = converter.convert(source)
                
                // then
                result.sort.orders shouldHaveSize 2
                result.sort.orders[0].property shouldBe "user.name"
                result.sort.orders[1].property shouldBe "user.profile.email"
            }
        }
    }
})
