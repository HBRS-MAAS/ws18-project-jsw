#CUSTOMER MESSAGES
##INPUT
The customer will receive input from the order processing agents of different bakeries mentioning the products and there price and the message will be of form:

```json
{
  "Product list":
  {
    "Bakery Name": "String-BakeryName",
    "ProductsOffered": {
     "Product":"Price"
                       }
    
    }
}

```
###Example

```json
{
  "Product list":
{
   {
    "Bakery Name":"Paris Baguette"
    "ProductOffered":
               {"apple pie":7,
                "swiss roll":10,
                "brownies":8,
                "cup bake":8,
                "croissant":10,
                "biscuit":10,
                "strudel":5,
                "muffin":5
                   }
         }
}
```

<br>

##OUTPUT
The customer sends orders to the bakery and the message is given by

```json
 {
    "Customerid": "String-customerid",
    "name": "String-customer-name",
    "location": {
      "y": "Integer-loc_y",
      "x": "Integer-loc_x"
    },
    "orders": [
      {
        "orderDate": {
          "day": "Integer-day",
          "hour": "Integer-hour"
        },
        "deliveryDate": {
          "day": "Integer-day",
          "hour": "Integer-hour"
        },
        "products": {
         "product name":"amount"
        }
      }
    ]
  }
```
###Example

```json
 {
    "Customerid": "AR-00333",
    "name": "Kings shop",
    "location": {
      "y": -1,
      "x": 3
    },
    "orders": [
      {
        "orderDate": {
          "day": 12,
          "hour": 5
        },
        "deliveryDate": {
          "day": 15,
          "hour": 8
        },
        "products": {
         "apple pie":7,
         "swiss roll":10,
         "brownies":8,
         "cup bake":8,
         "croissant":10
        }
      }
    ]
  }

```
<br>





