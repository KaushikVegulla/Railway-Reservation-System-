"""Malformed Cognito tokens fail before any network call."""

import unittest

from server import verify_cognito_jwt


class CognitoJwtTest(unittest.TestCase):
    def test_malformed_token(self):
        with self.assertRaises(ValueError):
            verify_cognito_jwt("not-a-jwt")


if __name__ == "__main__":
    unittest.main()
