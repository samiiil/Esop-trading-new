package controller

import models.*
import services.Util
import com.fasterxml.jackson.core.JsonParseException
import io.micronaut.core.convert.exceptions.ConversionErrorException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.hateoas.JsonError
import io.micronaut.web.router.exceptions.UnsatisfiedBodyRouteException
import services.Validations

@Controller("/")
class EndPoints {
    @Post("/user/register")
    fun register(@Body body: RegisterInput): HttpResponse<*> {
        val errorList = arrayListOf<String>()

        if (errorList.isNotEmpty()) {
            val response: Map<String, *>
            response = mapOf("error" to errorList)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        if(body.firstName.isNullOrBlank()){
            errorList.add("firstName is missing")
        }
        if(body.lastName.isNullOrBlank()){
            errorList.add("lastName is missing")
        }
        if(body.phoneNumber.isNullOrBlank()){
            errorList.add("phoneNumber is missing")
        }
        if(body.email.isNullOrBlank()){
            errorList.add("email is missing")
        }
        if(body.username.isNullOrBlank()){
            errorList.add("username is missing")
        }

        val firstName: String? = body.firstName
        val lastName: String? = body.lastName
        val phoneNumber: String? = body.phoneNumber
        val email: String? = body.email
        val username: String? = body.username

        for (error in Validations.validateFirstName(firstName)) errorList.add(error)
        for (error in Validations.validateLastName(lastName)) errorList.add(error)
        for (error in Validations.validatePhoneNumber(phoneNumber, errorList)) errorList.add(error)
        for (error in Validations.validateEmailIds(email)) errorList.add(error)
        for (error in Validations.validateUserName(username)) errorList.add(error)

        if (errorList.isEmpty()) {
            if (username != null && firstName!= null && lastName!= null &&  phoneNumber!= null && email!= null) {

                    Util.createUser(username, firstName, lastName, phoneNumber, email)

            }
        }

        val response: Map<String, *>
        if (errorList.isNotEmpty()) {
            response = mapOf("error" to errorList)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }
        val res = mutableMapOf<String, String>()
        res["firstName"] = firstName!!
        res["lastName"] = lastName!!
        res["phoneNumber"] = phoneNumber!!
        res["email"] = email!!
        res["username"] = username!!
        // response = mapOf("message" to "User created successfully!!")
        return HttpResponse.status<Any>(HttpStatus.OK).body(res)
    }

    @Post("/user/{username}/addToWallet")
    fun addToWallet(username: String, @Body body: AddToWalletInput): HttpResponse<*> {
        val errorMessages: ArrayList<String> = ArrayList<String>()
        //Input Parsing

        val amountToBeAdded: Long? = body.amount?.toLong()

        val response: Map<String, *>
        if (!Validations.validateUser(username)) {
            errorMessages.add("Username does not exists.")
        }

        if(amountToBeAdded == null){
            errorMessages.add("Amount field is missing")
        }

        if (errorMessages.isNotEmpty()) {
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        if(amountToBeAdded != null){
            if (amountToBeAdded + DataStorage.userList[username]!!.account.wallet.getFreeMoney() + DataStorage.userList[username]!!.account.wallet.getLockedMoney() <= 0 || amountToBeAdded + DataStorage.userList[username]!!.account.wallet.getFreeMoney() + DataStorage.userList[username]!!.account.wallet.getLockedMoney() >= Util.MAX_AMOUNT) {
                errorMessages.add("Invalid amount entered")
            }
            DataStorage.userList[username]!!.account.wallet.addMoneyToWallet(amountToBeAdded)
        }
        response = mapOf("message" to "$amountToBeAdded amount added to account")
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

    @Post("/user/{username}/addToInventory")
    fun addToInventory(username: String, @Body body: AddToInventoryInput): HttpResponse<*> {

        //Input Parsing
        val quantityToBeAdded = body.quantity.toLong()
        val typeOfESOP = body.esop_type?.uppercase() ?: "NON-PERFORMANCE"
        val errorMessages: ArrayList<String> = ArrayList()
        val response: Map<String, *>

        if (typeOfESOP !in arrayOf("PERFORMANCE", "NON-PERFORMANCE"))
            errorMessages.add("Invalid ESOP type")
        if (!Validations.validateUser(username)) {
            errorMessages.add("username does not exists.")
        } else {
            if (typeOfESOP == "NON-PERFORMANCE") {
                val totalQuantity =
                    quantityToBeAdded + DataStorage.userList[username]!!.account.inventory.getFreeInventory() + DataStorage.userList[username]!!.account.inventory.getLockedInventory()
                if (totalQuantity <= 0 || totalQuantity >= Util.MAX_AMOUNT) {
                    errorMessages.add("Invalid quantity entered")
                }
            } else if (typeOfESOP == "PERFORMANCE") {
                val totalQuantity =
                    quantityToBeAdded + DataStorage.userList[username]!!.account.inventory.getFreePerformanceInventory() + DataStorage.userList[username]!!.account.inventory.getLockedPerformanceInventory()
                if (totalQuantity <= 0 || totalQuantity >= Util.MAX_AMOUNT) {
                    errorMessages.add("Invalid quantity entered")
                }
            }
        }
        if (errorMessages.size > 0) {
            response = mapOf("error" to errorMessages)
            return HttpResponse.badRequest(response)
        }
        DataStorage.userList[username]!!.account.inventory.addEsopToInventory(quantityToBeAdded, typeOfESOP)

        response = mapOf("message" to "$quantityToBeAdded $typeOfESOP ESOPs added to account")
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

    @Get("/user/{username}/accountInformation")
    fun accountInformation(username: String): HttpResponse<*> {

        val errorMessages: ArrayList<String> = ArrayList()

        val response: Map<String, *>
        if (!Validations.validateUser(username)) {
            errorMessages.add("username does not exists.")
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        response = mapOf(
            "FirstName" to DataStorage.userList[username]!!.firstName,
            "LastName" to DataStorage.userList[username]!!.lastName,
            "Phone" to DataStorage.userList[username]!!.phoneNumber,
            "Email" to DataStorage.userList[username]!!.emailId,
            "Wallet" to mapOf(
                "free" to DataStorage.userList[username]!!.account.wallet.getFreeMoney(),
                "locked" to DataStorage.userList[username]!!.account.wallet.getLockedMoney()
            ),
            "Inventory" to arrayListOf<Any>(
                mapOf(
                    "esop_type" to "PERFORMANCE",
                    "free" to DataStorage.userList[username]!!.account.inventory.getFreePerformanceInventory(),
                    "locked" to DataStorage.userList[username]!!.account.inventory.getLockedPerformanceInventory()
                ),
                mapOf(
                    "esop_type" to "NON-PERFORMANCE",
                    "free" to DataStorage.userList[username]!!.account.inventory.getFreeInventory(),
                    "locked" to DataStorage.userList[username]!!.account.inventory.getLockedInventory()
                )
            )
        )
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

    @Post("/user/{username}/createOrder")
    fun createOrder(username: String, @Body body: CreateOrderInput): HttpResponse<*> {
        val errorMessages: ArrayList<String> = ArrayList()

        val response: Map<String, *>

        if (!Validations.validateUser(username))
            errorMessages.add("username does not exists.")
        if(body.order_type.isNullOrBlank())
            errorMessages.add("order_type is missing, order type should be BUY or SELL")
        if(body.price == null)
            errorMessages.add("price for the order is missing")
        if(body.quantity == null)
            errorMessages.add("quantity field for order is missing")
        if(body.order_type != null && body.order_type == "SELL" && body.esop_type.isNullOrBlank()){
            errorMessages.add("esop_type is missing, SELL order requires esop_type")
        }
        if(errorMessages.isNotEmpty()){
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        //Input Parsing
        val orderQuantity: Long? = body.quantity?.toLong()
        val orderType: String? = body.order_type?.trim()?.uppercase()
        val orderAmount: Long? = body.price?.toLong()
        val typeOfESOP: String = (body.esop_type ?: "NON-PERFORMANCE").trim().uppercase()

        if (orderType !in arrayOf("BUY", "SELL"))
            errorMessages.add("Invalid order type")
        if (typeOfESOP !in arrayOf("PERFORMANCE", "NON-PERFORMANCE"))
            errorMessages.add("Invalid type of ESOP, ESOP type should be PERFORMANCE or NON_PERFORMANCE")

        if(errorMessages.isEmpty() && orderAmount != null && orderType != null && orderQuantity != null ){
            //Create Order
            val result = DataStorage.userList[username]!!.addOrder(orderQuantity, orderType, orderAmount, typeOfESOP)
            if (result != "Order Placed Successfully.")
                errorMessages.add(result)
            else{
                val res = mutableMapOf<String, Any>()
                res["quantity"] = orderQuantity
                res["order_type"] = orderType
                res["price"] = orderAmount

                return HttpResponse.status<Any>(HttpStatus.OK).body(res)
            }

        }
        val res = mapOf("error" to errorMessages)
        return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(res)
    }

    @Get("/user/{username}/orderHistory")
    fun orderHistory(username: String): HttpResponse<*> {
        val errorMessages: ArrayList<String> = ArrayList()

        val response: Map<String, *>

        if (!Validations.validateUser(username)) {
            errorMessages.add("username does not exists.")
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        if (!Validations.validateUser(username)) {
            errorMessages.add("Username does not exists.")
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }
        response = DataStorage.userList[username]!!.getOrderDetails()
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

    @Get("/fees")
    fun getFees(): HttpResponse<*> {
        return HttpResponse.status<Any>(HttpStatus.OK)
            .body(mapOf(Pair("TotalFees", DataStorage.TOTAL_FEE_COLLECTED)))
    }

    @Error
    fun handleJsonSyntaxError(request: HttpRequest<*>, e: JsonParseException): MutableHttpResponse<out Any>? {
        //handles errors in json syntax
        val errorMap = mutableMapOf<String, ArrayList<String>>()
        val error = JsonError("Invalid JSON: ${e.message}")
        errorMap["error"] = arrayListOf<String>("Invalid JSON: ${e.message}")
        return HttpResponse.badRequest(errorMap)
    }

    //for handling missing fields in json input
    @Error
    fun handleBadRequest(request: HttpRequest<*>, e: ConversionErrorException): Any {
        println(e)
        val errorMessages = arrayOf("Add missing fields to the request")
        val response = mapOf("error" to errorMessages)
        return HttpResponse.status<Any>(HttpStatus.BAD_REQUEST).body(response)
    }

    @Error(exception = UnsatisfiedBodyRouteException::class)
    fun handleEmptyBody(request: HttpRequest<*>, e: UnsatisfiedBodyRouteException): HttpResponse<Map<String, Array<String>>>{
        return HttpResponse.badRequest(mapOf("error" to arrayOf("Request body is missing")))
    }
}